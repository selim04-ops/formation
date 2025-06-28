package com.esprit.formation.utils;

import com.esprit.formation.config.AccessDeniedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;

public class AuthChannelInterceptorAdapter implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Handle CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                accessor.setUser(auth); // Attach auth to session
            }
        }

        // Handle SUBSCRIBE
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Principal user = accessor.getUser();

            if (destination != null && user != null) {
                validateSubscription(destination, user);
            }
        }

        return message;
    }


    private void validateSubscription(String destination, Principal user) {
        // Example: Ensure users can only subscribe to their role-specific channels
        if (destination.startsWith("/topic/notifications/")) {
            String requiredRole = destination.replace("/topic/notifications/", "");
            if (!hasAuthority(user, requiredRole)) {
                throw new AccessDeniedException("Missing required role");
            }
        }
    }

    private boolean hasAuthority(Principal user, String role) {
        if (user instanceof Authentication) {
            return ((Authentication) user).getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(role));
        }
        return false;
    }
}

