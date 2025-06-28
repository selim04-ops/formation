package com.esprit.formation.services;

import com.esprit.formation.dto.NotificationDTO;
import com.esprit.formation.dto.NotificationData;
import com.esprit.formation.entities.Notification;
import com.esprit.formation.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public List<NotificationDTO> getNotificationsForRoles(List<String> roles, Long currentUserId) {
        return roles.stream()
                .flatMap(role -> notificationRepository.findByRole(role).stream())
                .filter(n -> n.getMessage() != null)
                .collect(Collectors.toMap(
                        Notification::getId,
                        n -> n,
                        (existing, replacement) -> existing
                ))
                .values().stream()
                .sorted(Comparator.comparing(Notification::getTimestamp).reversed())
                .map(n -> convertToDTO(n, currentUserId))
                .collect(Collectors.toList());
    }

    public NotificationDTO convertToDTO(Notification notification, Long currentUserId) {
        boolean isUnread = !notification.getUserIds().contains(currentUserId);

        return NotificationDTO.builder()
                .id(notification.getId())
                .actionType(notification.getAction())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .timestamp(notification.getTimestamp())
                .actorUsername(notification.getUsername())
                .data(NotificationData.builder()
                        .userEmail(notification.getAffectedUserEmail())
                        .userName(notification.getAffectedUserName())
                        .userRole(notification.getAffectedUserRole())
                        .isActive(notification.getAffectedUserActive())
                        .build())
                .userIds(notification.getUserIds())
                .isUnread(isUnread) // Add this field to DTO
                .build();
    }

    public void addUserIdToNotifications(List<String> notificationIds, Long userId) {
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);

        notifications.forEach(notification -> {
            if (!notification.getUserIds().contains(userId)) {
                notification.getUserIds().add(userId);
            }
        });

        notificationRepository.saveAll(notifications);
    }
}