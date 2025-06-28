package com.esprit.formation.entities;

public enum Role {
    ADMIN, PARTICIPANT, FORMATEUR, SUPER_ADMIN;


    // Validate if a string matches any of our roles (case-insensitive)
    public static boolean isValidRole(String roleName) {
        if (roleName == null) return false;

        try {
            // Convert input to uppercase and match against enum names
            Role.valueOf(roleName.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}