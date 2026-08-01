package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.ForgotPasswordRequest;
import com.autosalone.dtos.LoginRequest;
import com.autosalone.dtos.ResetPasswordRequest;
import com.autosalone.dtos.SignUpRequest;
import com.autosalone.enums.TokenType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.AuthToken;
import com.autosalone.models.User;
import com.autosalone.services.AuthTokenService;
import com.autosalone.services.EmailService;
import com.autosalone.services.UserService;

import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthController authController;

    private User mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        this.userId = UUID.randomUUID();
        this.mockUser = mock(User.class);
    }

    @Test
    void login_WithValidCredentials_Success() {
        LoginRequest request = new LoginRequest("test@email.com", "password123");
        when(userService.login(request.email(), request.password())).thenReturn(mockUser);

        Response response = authController.login(request);

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());

        verify(userService).login("test@email.com", "password123");
    }

    @Test
    void signUp_withValidToken_Success() {
        when(mockUser.getId()).thenReturn(userId);

        SignUpRequest request = new SignUpRequest("valid_token", "new_password");
        AuthToken token = new AuthToken("valid_token", mockUser, TokenType.REGISTRATION, null);

        when(authTokenService.validateToken(request.token(), TokenType.REGISTRATION)).thenReturn(token);

        Response response = authController.signUp(request);

        assertEquals(204, response.getStatus());
        verify(userService).activateUser(userId, "new_password");
        verify(authTokenService).deleteToken(token);
    }

    @Test
    void forgotPassword_WithExistingUser_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@email.com");
        AuthToken token = new AuthToken("reset_token", mockUser, TokenType.PASSWORD_RESET, null);

        when(userService.getUserByEmail(request.email())).thenReturn(mockUser);
        when(authTokenService.createPasswordResetToken(mockUser)).thenReturn(token);
        when(mockUser.getEmail()).thenReturn(request.email());

        Response response = authController.forgotPassword(request);

        assertEquals(204, response.getStatus());
        assertNull(response.getEntity()); // no body

        verify(authTokenService).createPasswordResetToken(mockUser);
        verify(emailService).sendPasswordReset("test@email.com", "reset_token");
    }

    @Test
    void forgotPassword_WithNonExistingUser_FakesSuccess() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("fake@email.com");

        doThrow(new ResourceNotFoundException("User not found")).when(userService).getUserByEmail(request.email());

        Response response = authController.forgotPassword(request);

        assertEquals(204, response.getStatus());
        verify(authTokenService, never()).createPasswordResetToken(any());
    }

    @Test
    void resetPassword_WithValidToken_Success() {
        when(mockUser.getId()).thenReturn(userId);

        ResetPasswordRequest request = new ResetPasswordRequest("reset_token", "new_password");
        AuthToken token = new AuthToken("reset_token", mockUser, TokenType.PASSWORD_RESET, null);

        when(authTokenService.validateToken(request.token(), TokenType.PASSWORD_RESET)).thenReturn(token);

        Response response = authController.resetPassword(request);

        assertEquals(204, response.getStatus());
        verify(userService).updatePassword(userId, "new_password");
        verify(authTokenService).deleteToken(token);
    }
}
