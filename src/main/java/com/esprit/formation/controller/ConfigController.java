package com.esprit.formation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/config")
public class ConfigController {

    @Value("${cors.allowed.origin}")
    private String allowedOrigin;

    @GetMapping("/backend-url")
    public ResponseEntity<Map<String, String>> getBackendUrl() {
        Map<String, String> response = new HashMap<>();
        response.put("baseUrl", allowedOrigin); // or whatever the backend base URL is
        return ResponseEntity.ok(response);
    }
}

