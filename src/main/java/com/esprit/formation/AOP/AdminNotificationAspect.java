package com.esprit.formation.AOP;


import com.esprit.formation.dto.AuthRequest;
import com.esprit.formation.dto.UserDTO;
import com.esprit.formation.entities.Notification;
import com.esprit.formation.entities.User;
import com.esprit.formation.repository.NotificationRepository;
import com.esprit.formation.services.NotificationService;
import com.esprit.formation.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminNotificationAspect {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final UserService userService;

    // Pointcut for all admin endpoints
    @Pointcut("within(com.esprit.formation.controller.UserController) && " +
            "@annotation(operation) && " +
            "@annotation(preAuthorize)")
    public void adminOperations(Operation operation, PreAuthorize preAuthorize) {}

    @AfterReturning(pointcut = "adminOperations(operation, preAuthorize)", returning = "response")
    public void logSuccess(JoinPoint joinPoint, Operation operation,
                           PreAuthorize preAuthorize, ResponseEntity<?> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            createNotification(joinPoint, operation, preAuthorize, "SUCCESS", null);
        }
    }

    @AfterThrowing(pointcut = "adminOperations(operation, preAuthorize)", throwing = "ex")
    public void logFailure(JoinPoint joinPoint, Operation operation,
                           PreAuthorize preAuthorize, Exception ex) {
        createNotification(joinPoint, operation, preAuthorize, "FAILED", ex.getMessage());
    }

    private void createNotification(JoinPoint joinPoint, Operation operation,
                                    PreAuthorize preAuthorize, String status, String error) {
        // 1. Extract method info
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();

        // 2. Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "SYSTEM";

        // 3. Determine recipient roles from @PreAuthorize
        String recipientRoles = extractRolesFromPreAuthorize(preAuthorize.value());

        // 4. Create and save notification
        Notification notification = Notification.builder()
                .action(operation.summary())
                .message(createFriendlyMessage(operation.summary(), joinPoint.getArgs(), status, error))
                .status(status)
                .timestamp(Instant.now())
                .username(username)
                .recipientRoles(recipientRoles)
                .method(signature.getName())
                .endpoint(request.getRequestURI())
                .build();

        // Extract and set affected user data if available
        extractAffectedUserData(joinPoint.getArgs(), notification);

        notificationRepository.save(notification);
        sendWebSocketNotification(notification);
    }
    private String createFriendlyMessage(String action, Object[] args, String status, String error) {
        switch (action) {
            case "Ajouter un utilisateur":
                return "User added";
            case "Supprimer un utilisateur":
                return "User deleted";
            case "Mise à jour utilisateur":
                return "User updated";
            case "Mettre à jour l'état actif":
                return "User status changed";
            case "Voir tous les utilisateurs":
                return null; // Will be filtered out
            default:
                return action;
        }
    }
    private void extractAffectedUserData(Object[] args, Notification notification) {
        for (Object arg : args) {
            if (arg instanceof UserDTO) {
                UserDTO user = (UserDTO) arg;
                notification.setAffectedUserEmail(user.getEmail());
                notification.setAffectedUserName(user.getNomEtPrenom());
                notification.setAffectedUserRole(user.getRole());
                notification.setAffectedUserActive(user.getIsActive());
            } else if (arg instanceof AuthRequest) {
                AuthRequest auth = (AuthRequest) arg;
                notification.setAffectedUserEmail(auth.getEmail());
                notification.setAffectedUserActive(auth.getIsActive());
            } else if (arg instanceof String && arg.toString().contains("@")) {
                // For delete operations where email is passed directly
                notification.setAffectedUserEmail(arg.toString());
            }
        }
    }

    private String extractRolesFromPreAuthorize(String preAuthorizeValue) {
        // Extract roles from @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
        return preAuthorizeValue.replaceAll(".*\\('(.*)'\\).*", "$1");
    }

    private String buildDescription(Object[] args, String status, String error) {
        StringBuilder sb = new StringBuilder();
        sb.append("Status: ").append(status);
        if (error != null) {
            sb.append(" | Error: ").append(error);
        }
        sb.append(" | Args: ").append(Arrays.toString(args));
        return sb.toString();
    }

    private void sendWebSocketNotification(Notification notification) {
        // Get users who have the required roles AND are currently connected
        List<String> authorizedUsernames = userService.findConnectedUsersByRoles(
                Arrays.asList(notification.getRecipientRoles().split(","))
        );

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = ((User) auth.getPrincipal()).getId();

        authorizedUsernames.forEach(username -> {
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/notifications",
                    notificationService.convertToDTO(notification, userId)
            );
        });
    }


}