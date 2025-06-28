package com.esprit.formation.iservices;

import com.esprit.formation.entities.RevokedToken;
import com.esprit.formation.entities.User;

public interface IAuthService {
     User extractUserFromToken(String token);

    // Method to register a new user
    User enregistrer(User user);

    // Method to authenticate a user and generate a JWT token
    String authenticate(String email, String password);

    // methode to allow user to use its profile
    boolean verifyEmailAndCode(String email, String code);

    String tokenVerification(String token);

    }
