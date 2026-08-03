package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.PasswordUpdateRequest;
import com.autosalone.dtos.UserResponse;
import com.autosalone.enums.TokenType;
import com.autosalone.models.AuthToken;
import com.autosalone.models.User;
import com.autosalone.services.AuthTokenService;
import com.autosalone.services.EmailService;
import com.autosalone.services.UserService;

import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserController userController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void getUserById_Returns200AndUser() {
        User mockUser = mock(User.class);
        when(userService.getUserById(userId)).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn(userId);
        when(mockUser.getEmail()).thenReturn("test@email.com");

        Response response = userController.getUserById(userId);

        assertEquals(200, response.getStatus());

        UserResponse userResponse = (UserResponse) response.getEntity();
        assertEquals(userId, userResponse.id());
        assertEquals("test@email.com", userResponse.email());

        verify(userService).getUserById(userId);
    }

    @Test
    void updatePassword_Returns204NoContent() {
        PasswordUpdateRequest request = new PasswordUpdateRequest("currentPassword", "newPassword");

        Response response = userController.updatePassword(userId, request);

        assertEquals(204, response.getStatus());
        assertNull(response.getEntity()); // no body
        verify(userService).updatePassword(userId, "currentPassword", "newPassword");
    }

    @Test
    void deactivateUser_Returns204NoContent() {
        Response response = userController.deactivateUser(userId);

        assertEquals(204, response.getStatus());
        assertNull(response.getEntity()); // no body
        verify(userService).deactivateUser(userId);
    }

    @Test
    void sendRegistrationInvite_Returns204NoContent() {
        User mockUser = mock(User.class);
        when(mockUser.getEmail()).thenReturn("test@email.com");

        AuthToken token = new AuthToken("token_123", mockUser, TokenType.REGISTRATION, null);

        when(userService.getUserById(userId)).thenReturn(mockUser);
        when(authTokenService.createRegistrationToken(mockUser)).thenReturn(token);

        Response response = userController.sendRegistrationInvite(userId);

        assertEquals(204, response.getStatus());
        assertNull(response.getEntity()); // no body

        verify(userService).getUserById(userId);
        verify(authTokenService).createRegistrationToken(mockUser);
        verify(emailService).sendRegistrationInvite("test@email.com", "token_123");
    }
}