package com.esprit.formation.config;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveUser {
    public Map<String, String> activeUsers = new ConcurrentHashMap<>();
}

