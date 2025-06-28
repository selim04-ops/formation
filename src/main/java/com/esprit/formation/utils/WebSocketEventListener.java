package com.esprit.formation.utils;

import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WebSocketEventListener {
    private final WebSocketSessionRegistry sessionRegistry;

    public WebSocketEventListener(WebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        if (event.getMessage() == null) return;
        Principal principal = event.getUser();
        if (principal != null) {
            String username = principal.getName();
            List<String> roles = extractRolesFromAuthentication(principal);
            if (!roles.isEmpty()) {
                String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
                sessionRegistry.registerSession(sessionId, username, roles);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        sessionRegistry.removeSession(sessionId);
    }

    private List<String> extractRolesFromAuthentication(Principal principal) {
        if (principal instanceof Authentication) {
            return ((Authentication) principal).getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}