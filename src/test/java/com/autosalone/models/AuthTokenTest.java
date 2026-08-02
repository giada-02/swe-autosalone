package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.autosalone.enums.TokenType;

class AuthTokenTest {

    private User user;

    @BeforeEach
    public void setUp() {
        this.user = new Customer.CustomerBuilder().setFirstName("Mario").setLastName("Rossi")
                .setPhoneNumber("3331234567").build();
    }

    @Test
    public void isExpired_ReturnsTrue_WhenDateIsInThePast() {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        AuthToken token = new AuthToken("token_123", user, TokenType.REGISTRATION, past);

        assertTrue(token.isExpired(), "The token must be expired");
    }

    @Test
    public void isExpired_ReturnsFalse_WhenDateIsInTheFuture() {
        Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
        AuthToken token = new AuthToken("token_123", user, TokenType.REGISTRATION, future);

        assertFalse(token.isExpired(), "The token should not be expired");
    }
}