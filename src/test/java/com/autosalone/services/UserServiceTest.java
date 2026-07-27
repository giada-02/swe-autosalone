package com.autosalone.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.User;
import com.autosalone.repositories.UserRepository;

import jakarta.security.enterprise.identitystore.PasswordHash;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHash passwordHash;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User mockUser;
    private String email;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = "test@autosalone.com";
        mockUser = mock(User.class);
    }

    // read

    @Test
    void getUserById_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        User result = userService.getUserById(userId);
        assertNotNull(result);
        assertEquals(mockUser, result);
    }

    @Test
    void getUserById_NotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(userId);
        });
    }

    @Test
    void getUserByEmail_Success() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        User result = userService.getUserByEmail(email);
        assertNotNull(result);
        assertEquals(mockUser, result);
    }

    @Test
    void getUserByEmail_NotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserByEmail(email);
        });
    }

    // write

    @Test
    void updateEmail_Success_EmailNotUsed() {
        String newEmail = "new@autosalone.com";

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(mockUser.getEmail()).thenReturn(email);
        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.empty());

        userService.updateEmail(userId, newEmail);

        verify(mockUser).setEmail(newEmail);
        verify(userRepository).save(mockUser);
    }

    @Test
    void updateEmail_Success_EmailUsedBySameUser() {
        String newEmail = "same@autosalone.com";

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(mockUser.getEmail()).thenReturn(newEmail);

        userService.updateEmail(userId, newEmail);

        verify(mockUser, never()).setEmail(newEmail);
        verify(userRepository, never()).save(mockUser);
    }

    @Test
    void updateEmail_FailsIfEmailUsedByAnotherUser() {
        String newEmail = "taken@autosalone.com";

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(mockUser.getEmail()).thenReturn(email);

        User anotherUser = mock(User.class);
        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.of(anotherUser));

        assertThrows(IllegalStateException.class, () -> {
            userService.updateEmail(userId, newEmail);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePassword_Success() {
        String rawPassword = "password";
        String hashedPassword = "hashed_password";

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(passwordHash.generate(any())).thenReturn(hashedPassword);

        userService.updatePassword(userId, rawPassword);

        verify(mockUser).setPassword(hashedPassword);
        verify(userRepository).save(mockUser);
    }

    @Test
    void activateUser_Success() {
        String rawPassword = "password";
        String hashedPassword = "hashed_password";

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(mockUser.isActive()).thenReturn(false);
        when(passwordHash.generate(any())).thenReturn(hashedPassword);

        userService.activateUser(userId, rawPassword);

        verify(mockUser).setPassword(hashedPassword);
        verify(mockUser).activate();
        verify(userRepository).save(mockUser);
    }

    @Test
    void activateUser_FailsIfAlreadyActive() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(mockUser.isActive()).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> {
            userService.activateUser(userId, "password");
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivateUser_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        userService.deactivateUser(userId);

        verify(mockUser).deactivate();
        verify(mockUser).setPassword(null);
        verify(userRepository).save(mockUser);
    }

    // security

    @Test
    void verifyPassword_Success() {
        String rawPassword = "password";
        String hashedPassword = "hashed_password";

        when(passwordHash.verify(any(), eq(hashedPassword))).thenReturn(true);

        boolean isCorrect = userService.verifyPassword(rawPassword, hashedPassword);

        assertTrue(isCorrect);
    }

    @Test
    void login_Success() {
        String rawPassword = "password";
        String hashedPassword = "hashed_password";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getPassword()).thenReturn(hashedPassword);
        when(passwordHash.verify(any(), eq(hashedPassword))).thenReturn(true);

        User loggedUser = userService.login(email, rawPassword);

        assertNotNull(loggedUser);
        assertEquals(mockUser, loggedUser);
    }

    @Test
    void login_FailsIfUserNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            userService.login(email, "password");
        });

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void login_FailsIfNotActive() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(mockUser.isActive()).thenReturn(false);

        assertThrows(SecurityException.class, () -> {
            userService.login(email, "password");
        });
    }

    @Test
    void login_FailsIfNoPassword() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getPassword()).thenReturn(null);

        assertThrows(SecurityException.class, () -> {
            userService.login(email, "password");
        });
    }

    @Test
    void login_FailsIfWrongPassword() {
        String rawPassword = "wrong_password";
        String hashedPassword = "hashed_password";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getPassword()).thenReturn(hashedPassword);

        when(passwordHash.verify(any(char[].class), eq(hashedPassword))).thenReturn(false);

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            userService.login(email, rawPassword);
        });

        assertEquals("Invalid credentials", exception.getMessage());
    }
}