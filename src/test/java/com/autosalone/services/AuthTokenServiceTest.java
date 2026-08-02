package com.autosalone.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.enums.TokenType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.AuthToken;
import com.autosalone.models.User;
import com.autosalone.repositories.AuthTokenRepository;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private AuthTokenRepository tokenRepository;

    @InjectMocks
    private AuthTokenService authTokenService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
    }

    @Test
    void createRegistrationToken_ShouldGenerateValidTokenFor48Hours() {
        when(mockUser.getEmail()).thenReturn("test@email.com");
        AuthToken token = authTokenService.createRegistrationToken(mockUser);

        assertNotNull(token.getToken());
        assertEquals(TokenType.REGISTRATION, token.getType());
        assertEquals(mockUser, token.getUser());
        verify(tokenRepository).save(any(AuthToken.class));
    }

    @Test
    void createPasswordResetToken_ShouldGenerateValidTokenFor48Hours() {
        when(mockUser.getEmail()).thenReturn("test@email.com");
        when(mockUser.isActive()).thenReturn(true);
        AuthToken token = authTokenService.createPasswordResetToken(mockUser);

        assertNotNull(token.getToken());
        assertEquals(TokenType.PASSWORD_RESET, token.getType());
        assertEquals(mockUser, token.getUser());
        verify(tokenRepository).save(any(AuthToken.class));
    }

    @Test
    void validateToken_WhenValid_Success() {
        String tokenString = "secure_token_123";
        AuthToken validToken = new AuthToken(tokenString, mockUser, TokenType.PASSWORD_RESET,
                Instant.now().plus(2, ChronoUnit.HOURS));

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.of(validToken));

        AuthToken result = authTokenService.validateToken(tokenString, TokenType.PASSWORD_RESET);

        assertEquals(validToken, result);
    }

    @Test
    void validateToken_WhenInvalid_NotFound() {
        String tokenString = "invalid_token";
        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authTokenService.validateToken(tokenString, TokenType.REGISTRATION));
    }

    @Test
    void validateToken_FailsWithInvalidTokenType() {
        String tokenString = "secure_token_123";
        AuthToken resetToken = new AuthToken(tokenString, mockUser, TokenType.PASSWORD_RESET,
                Instant.now().plus(2, ChronoUnit.HOURS));

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.of(resetToken));

        assertThrows(IllegalArgumentException.class,
                () -> authTokenService.validateToken(tokenString, TokenType.REGISTRATION));
    }

    @Test
    void validateToken_FailsWhenExpired() {
        String tokenString = "secure_token_123";
        AuthToken expiredToken = new AuthToken(tokenString, mockUser, TokenType.REGISTRATION,
                Instant.now().minus(1, ChronoUnit.HOURS));

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.of(expiredToken));

        assertThrows(IllegalArgumentException.class,
                () -> authTokenService.validateToken(tokenString, TokenType.REGISTRATION));
    }
}