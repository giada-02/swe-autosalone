package com.autosalone.services;

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
        userRepository.findByEmail(newEmail).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(userId)) {
                throw new IllegalStateException("Email is already in use by another account");
            }
        });

        User user = getUserById(userId);

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
        User user = getUserByEmail(email);

        if (!user.isActive()) {
            throw new SecurityException("This user in not active");
        }

        if (user.getPassword() == null) {
            throw new SecurityException("This user has not been configured, must activate and set a password to login");
        }

        boolean isPasswordCorrect = verifyPassword(rawPassword, user.getPassword());
        if (!isPasswordCorrect) {
            throw new IllegalArgumentException("The provided creadentials are not valid");
        }

        return user;
    }
}
