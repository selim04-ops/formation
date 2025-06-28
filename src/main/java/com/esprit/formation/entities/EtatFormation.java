package com.esprit.formation.entities;

public enum EtatFormation {
    A_VENIR("à venir"),
    EN_COURS("en cours"),
    TERMINEE("terminée");

    private final String displayValue;

    EtatFormation(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}
