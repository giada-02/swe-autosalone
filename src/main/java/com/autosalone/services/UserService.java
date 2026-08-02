package com.autosalone.services;

import java.util.UUID;

import com.autosalone.enums.TokenType;
import com.autosalone.exceptions.ForbiddenException;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.exceptions.UnauthorizedException;
import com.autosalone.models.AuthToken;
import com.autosalone.models.User;
import com.autosalone.repositories.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.PasswordHash;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserService {

    @Inject
    private UserRepository userRepository;

    @Inject
    private AuthTokenService authTokenService;

    @Inject
    private PasswordHash passwordHash;

    // read

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found of id: " + id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found of email: " + email));
    }

    // write

    @Transactional
    public void updateEmail(UUID userId, String newEmail) {
        User user = getUserById(userId);

        if (user.getEmail().equals(newEmail)) {
            return;
        }

        userRepository.findByEmail(newEmail).ifPresent(existing -> {
            throw new IllegalStateException("Email is already in use by another account");
        });

        user.setEmail(newEmail);

        userRepository.save(user);
    }

    @Transactional
    public void updatePassword(UUID userId, String rawCurrentPassword, String rawNewPassword) {
        User user = getUserById(userId);

        boolean isCurrentPasswordCorrect = verifyPassword(rawCurrentPassword, user.getPassword());

        if (!isCurrentPasswordCorrect) {
            throw new ForbiddenException("The current password is wrong");
        }

        String hashedPassword = passwordHash.generate(rawNewPassword.toCharArray());
        user.setPassword(hashedPassword);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(UUID userId, String rawNewPassword) {
        User user = getUserById(userId);

        if (user.getPassword() != null) {
            throw new IllegalStateException("The user has already been configured");
        }

        String hashedPassword = passwordHash.generate(rawNewPassword.toCharArray());
        user.setPassword(hashedPassword);
        user.activate();

        userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(UUID userId) {
        User user = getUserById(userId);
        user.deactivate();
        user.setPassword(null);
        userRepository.save(user);
    }

    // security

    public boolean verifyPassword(String rawPassword, String hashedDatabasePassword) {
        return passwordHash.verify(rawPassword.toCharArray(), hashedDatabasePassword);
    }

    public User login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.getPassword() == null) {
            throw new UnauthorizedException("Invalid credentials");
        }

        boolean isPasswordCorrect = verifyPassword(rawPassword, user.getPassword());
        if (!isPasswordCorrect) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new ForbiddenException(
                    "This user has not been configured yet, must activate to login");
        }

        return user;
    }

    @Transactional
    public void completeRegistration(String tokenString, String newPassword) {

        AuthToken token = authTokenService.validateToken(tokenString, TokenType.REGISTRATION);

        activateUser(token.getUser().getId(), newPassword);

        authTokenService.deleteToken(token);
    }

    @Transactional
    public void completePasswordReset(String tokenString, String newPassword) {

        AuthToken token = authTokenService.validateToken(tokenString, TokenType.PASSWORD_RESET);

        User user = token.getUser();
        String hashedPassword = passwordHash.generate(newPassword.toCharArray());
        user.setPassword(hashedPassword);
        userRepository.save(user);

        authTokenService.deleteToken(token);
    }
}