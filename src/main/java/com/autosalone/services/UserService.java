package com.autosalone.services;

import java.util.Optional;
import java.util.UUID;

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
    private PasswordHash passwordHash;

    // read

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found of id: " + id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found of email: " + email));
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
    public void updatePassword(UUID userId, String rawNewPassword) {
        User user = getUserById(userId);

        String hashedPassword = passwordHash.generate(rawNewPassword.toCharArray());

        user.setPassword(hashedPassword);

        userRepository.save(user);
    }

    @Transactional
    public void activateUser(UUID userId, String rawNewPassword) {
        User user = getUserById(userId);

        if (user.isActive()) {
            throw new IllegalStateException("The user is active already");
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
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new SecurityException("Invalid credentials");
        }

        User user = optionalUser.get();

        if (!user.isActive()) {
            throw new SecurityException("This user is not active");
        }

        if (user.getPassword() == null) {
            throw new SecurityException("This user has not been configured, must activate and set a password to login");
        }

        boolean isPasswordCorrect = verifyPassword(rawPassword, user.getPassword());

        if (!isPasswordCorrect) {
            throw new SecurityException("Invalid credentials");
        }

        return user;
    }
}
