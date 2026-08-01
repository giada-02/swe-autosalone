package com.autosalone.services;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import com.autosalone.enums.TokenType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.AuthToken;
import com.autosalone.models.User;
import com.autosalone.repositories.AuthTokenRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuthTokenService {

    @Inject
    private AuthTokenRepository authTokenRepository;

    private static final int REGISTRATION_TOKEN_EXPIRATION_HOURS = 48;
    private static final int RESET_TOKEN_EXPIRATION_HOURS = 2;

    @Transactional
    public AuthToken createRegistrationToken(User user) {

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalStateException(
                    "To send a registration invitation to the user, they must have an email address");
        }

        if (user.isActive() || user.getPassword() != null) {
            throw new IllegalStateException("The user has already completed the registration in the past");
        }

        Optional<AuthToken> maybeExistingToken = authTokenRepository.findByUserAndType(user, TokenType.REGISTRATION);
        if (maybeExistingToken.isPresent()) {
            AuthToken existingToken = maybeExistingToken.get();
            if (!existingToken.isExpired()) {
                throw new IllegalStateException(
                        "There's an active registration token already for this user, wait for its expiration");
            } else {
                authTokenRepository.delete(existingToken);
            }
        }

        String tokenString = generateSecureToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(REGISTRATION_TOKEN_EXPIRATION_HOURS);

        AuthToken token = new AuthToken(tokenString, user, TokenType.REGISTRATION, expiryDate);
        authTokenRepository.save(token);

        return token;
    }

    @Transactional
    public AuthToken createPasswordResetToken(User user) {
        String tokenString = generateSecureToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRATION_HOURS);

        AuthToken token = new AuthToken(tokenString, user, TokenType.PASSWORD_RESET, expiryDate);
        authTokenRepository.save(token);

        return token;
    }

    public AuthToken validateToken(String tokenString, TokenType expectedType) {
        AuthToken token = authTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or non-existent token"));

        if (token.getType() != expectedType) {
            throw new IllegalArgumentException("Invalid token type");
        }

        if (token.isExpired()) {
            throw new IllegalArgumentException("Expired token");
        }

        return token;
    }

    @Transactional
    public void deleteToken(AuthToken token) {
        authTokenRepository.delete(token);
    }

    @Transactional
    public void deleteExpiredAuthTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = authTokenRepository.deleteAllExpiredTokens(now);
        System.out.println("Deleted " + deletedCount + " expired tokens");
    }

    /**
     * Genera una stringa casuale sicura a 256 bit e codificata in Base64 (URL
     * Safe).
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}