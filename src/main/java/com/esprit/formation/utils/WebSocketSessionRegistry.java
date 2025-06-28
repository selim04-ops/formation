package com.esprit.formation.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WebSocketSessionRegistry {
    // Key: sessionId, Value: username
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    // Key: username, Value: roles
    private final Map<String, List<String>> userRoles = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, String username, List<String> roles) {
        activeSessions.put(sessionId, username);
        userRoles.put(username, roles);
    }

    public void removeSession(String sessionId) {
        String username = activeSessions.remove(sessionId);
        if (username != null) {
            userRoles.remove(username);
        }
    }

    public List<String> findUsernamesByRoles(List<String> targetRoles) {
        return userRoles.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(targetRoles::contains))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}