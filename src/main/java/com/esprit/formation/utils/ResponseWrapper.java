package com.esprit.formation.utils;

import com.esprit.formation.dto.UserDTO;
import com.esprit.formation.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

public class ResponseWrapper {


    private final PasswordEncoder passwordEncoder;

    public ResponseWrapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public static ResponseEntity<Object> success(Object data) {
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    public static ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }

}
