package com.esprit.formation.services;

import com.esprit.formation.entities.PasswordResetToken;
import com.esprit.formation.entities.User;

import com.esprit.formation.iservices.IPasswordResetTokenService;
import com.esprit.formation.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetTokenService implements IPasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Override
    public PasswordResetToken createPasswordResetTokenForUser(User user, String token) {
        PasswordResetToken myToken = new PasswordResetToken();
        myToken.setToken(token);
        myToken.setUser(user);
        myToken.setExpiryDate(calculateExpiryDate());
        return passwordResetTokenRepository.save(myToken);
    }
    @Override
    public PasswordResetToken getToken(String token) {
        return passwordResetTokenRepository.findByToken(token).orElse(null);
    }
@Override
    public PasswordResetToken getByUser(User user) {
        return passwordResetTokenRepository.findByUser(user).orElse(null);
    }

    @Override
    public String checkForToken(User user) {
        // Check for existing active token
        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findActiveTokenByUserId(user.getId());

        if (existingToken.isPresent()) {
            // Return existing active token
            return existingToken.get().getToken();
        }

        // No active token exists - create new one
        String newToken = UUID.randomUUID().toString();
        PasswordResetToken freshToken = PasswordResetToken.builder()
                .token(newToken)
                .user(user)
                .expiryDate(calculateExpiryDate()) // 24 hours validity
                .isAvailable(true)
                .build();

        passwordResetTokenRepository.save(freshToken);
        return newToken;
    }



    @Override
    public PasswordResetToken updatePasswordResetToken(PasswordResetToken passwordResetToken) {
        passwordResetToken.setAvailable(false);
        return passwordResetTokenRepository.save(passwordResetToken);
    }

    @Override
    public void deleteOldTokens(User user) {
        List<PasswordResetToken> oldTokens = passwordResetTokenRepository.findByUserIdAndIsAvailableFalse(user.getId());
        passwordResetTokenRepository.deleteAll(oldTokens);
    }

private Date calculateExpiryDate() {
    int expiryDate = 86400000;
    return new Date(System.currentTimeMillis() +  expiryDate);
    }
}