package com.esprit.formation.entities;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.text.Normalizer;
import java.util.Arrays;

public enum NiveauFormation {
    DEBUTANT, INTERMEDIAIRE, AVANCE;



    @JsonCreator
    public static NiveauFormation fromString(String value) {
        if (value == null) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toUpperCase();
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid NiveauFormation value: " + value +
                    ". Accepted values: " + Arrays.toString(values()));
        }
    }
}

