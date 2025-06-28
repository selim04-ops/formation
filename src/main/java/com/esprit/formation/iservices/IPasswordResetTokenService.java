package com.esprit.formation.iservices;

import com.esprit.formation.entities.PasswordResetToken;
import com.esprit.formation.entities.User;

public interface IPasswordResetTokenService {
     PasswordResetToken createPasswordResetTokenForUser(User user, String token);
     PasswordResetToken getToken(String token);
     PasswordResetToken updatePasswordResetToken(PasswordResetToken passwordResetToken);
     PasswordResetToken getByUser(User user);
     String checkForToken(User user);
     void deleteOldTokens(User user);


     }
