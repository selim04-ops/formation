package com.esprit.formation.services;

import com.esprit.formation.entities.User;
import com.esprit.formation.iservices.IAuthService;
import com.esprit.formation.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JavaMailSenderImpl mailSender;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private final UserService userService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, JavaMailSenderImpl mailSender, UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailSender = mailSender;
        this.userService = userService;
    }

    private final Map<String, String> emailToCodeMap = new HashMap<>();

// enregistrement de user (isActive false) et verifier son email by code-----------------------
    @Override
    public User enregistrer(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return null;
        }
        String code = generateRandomCode();
        emailToCodeMap.put(user.getEmail(), code);
        LOGGER.info("Generated code for email {} : {}", user.getEmail(), code);
        sendVerificationEmail(user.getEmail(), code);
        user.setMotDePasse(passwordEncoder.encode(user.getMotDePasse()));
        user.setIsActive(false);
        return userRepository.save(user);
    }
    @Override
    public boolean verifyEmailAndCode(String email, String code) {
        String storedCode = emailToCodeMap.get(email);
        if (storedCode != null && storedCode.equals(code)) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setIsActive(true);
            userRepository.save(user);
            emailToCodeMap.remove(email);
            return true;
        }
        return false;
    }

    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void sendVerificationEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom("nhoucem44@zohomail.com");
        message.setSubject("Your Verification Code");
        message.setText("Your verification code is: " + code);
        mailSender.send(message);
    }

//to authenticate user ------------------------------------------------------------------------
    @Override
    public String authenticate(String email, String password) {
    Optional<User> userOpt = userRepository.findByEmail(email);
    if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getMotDePasse())) {
        return jwtService.generateToken(userOpt.get());
    }
    return null;
}

// other methode----------------------------------------------------------------
    @Override
    public User extractUserFromToken(String token){
        String email = jwtService.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElse(null);
    }

    @Override
    public String tokenVerification(String token) {
        if (jwtService.isTokenExpired(token)) {
            userService.creatRevokedToken(token); // Revoke expired tokens
            throw new RuntimeException("Token is expired");
        }

        if (userService.isTokenRevoked(token)) {
            throw new RuntimeException("Token is revoked");
        }

        return token; // Return the token if valid
    }
}

