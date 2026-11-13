package com.autosalone.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.config.ClockProducer;
import com.autosalone.enums.TokenType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.AuthToken;
import com.autosalone.models.User;
import com.autosalone.repositories.AuthTokenRepository;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");

    @Spy
    private Clock clock = Clock.fixed(NOW, ClockProducer.TIME_ZONE_ID);

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
    void createRegistrationToken_GeneratesRegistrationToken() {
        when(mockUser.getEmail()).thenReturn("test@email.com");
        AuthToken token = authTokenService.createRegistrationToken(mockUser);

        assertNotNull(token.getToken());
        assertEquals(TokenType.REGISTRATION, token.getType());
        assertEquals(mockUser, token.getUser());
        assertEquals(NOW.plus(48, ChronoUnit.HOURS), token.getExpiryDate());
        verify(tokenRepository).save(any(AuthToken.class));
    }

    @Test
    void createPasswordResetToken_GeneratesPasswordResetToken() {
        when(mockUser.getEmail()).thenReturn("test@email.com");
        when(mockUser.isActive()).thenReturn(true);
        AuthToken token = authTokenService.createPasswordResetToken(mockUser);

        assertNotNull(token.getToken());
        assertEquals(TokenType.PASSWORD_RESET, token.getType());
        assertEquals(mockUser, token.getUser());
        assertEquals(NOW.plus(2, ChronoUnit.HOURS), token.getExpiryDate());
        verify(tokenRepository).save(any(AuthToken.class));
    }

    @Test
    void createPasswordResetToken_WithinCooldown_ReturnsNull() {
        when(mockUser.getEmail()).thenReturn("test@email.com");
        when(mockUser.isActive()).thenReturn(true);

        AuthToken existingToken = mock(AuthToken.class);
        when(existingToken.getCreatedAt()).thenReturn(NOW.minus(3, ChronoUnit.MINUTES));
        when(tokenRepository.findByUserAndType(mockUser, TokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(existingToken));

        AuthToken token = authTokenService.createPasswordResetToken(mockUser);

        assertNull(token);
        verify(tokenRepository, never()).delete(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void createPasswordResetToken_AfterCooldown_ReplacesExistingToken() {
        when(mockUser.getEmail()).thenReturn("test@email.com");
        when(mockUser.isActive()).thenReturn(true);

        AuthToken existingToken = mock(AuthToken.class);
        when(existingToken.getCreatedAt()).thenReturn(NOW.minus(6, ChronoUnit.MINUTES));
        when(tokenRepository.findByUserAndType(mockUser, TokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(existingToken));

        AuthToken token = authTokenService.createPasswordResetToken(mockUser);

        assertNotNull(token);
        assertEquals(TokenType.PASSWORD_RESET, token.getType());
        verify(tokenRepository).delete(existingToken);
        verify(tokenRepository).save(token);
    }

    @Test
    void validateToken_WhenValid_Success() {
        String tokenString = "secure_token_123";
        AuthToken validToken = new AuthToken(tokenString, mockUser, TokenType.PASSWORD_RESET,
                NOW.plus(2, ChronoUnit.HOURS));

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
                NOW.plus(2, ChronoUnit.HOURS));

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.of(resetToken));

        assertThrows(IllegalArgumentException.class,
                () -> authTokenService.validateToken(tokenString, TokenType.REGISTRATION));
    }

    @Test
    void validateToken_FailsWhenExpired() {
        String tokenString = "secure_token_123";
        AuthToken expiredToken = new AuthToken(tokenString, mockUser, TokenType.REGISTRATION,
                NOW.minus(1, ChronoUnit.HOURS));

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.of(expiredToken));

        assertThrows(IllegalArgumentException.class,
                () -> authTokenService.validateToken(tokenString, TokenType.REGISTRATION));
    }

    @Test
    void deleteToken_Success() {
        AuthToken mockToken = mock(AuthToken.class);

        authTokenService.deleteToken(mockToken);

        verify(tokenRepository).delete(mockToken);
    }

    @Test
    void deleteExpiredAuthTokens_ReturnsDeletedCount() {
        int expectedDeletedCount = 5;
        when(tokenRepository.deleteAllExpiredTokens(NOW)).thenReturn(expectedDeletedCount);

        int actualDeletedCount = authTokenService.deleteExpiredAuthTokens();

        assertEquals(expectedDeletedCount, actualDeletedCount);
        verify(tokenRepository).deleteAllExpiredTokens(NOW);
    }
}