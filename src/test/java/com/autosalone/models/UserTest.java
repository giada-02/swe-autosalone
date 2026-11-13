package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.autosalone.models.Customer.CustomerBuilder;

public class UserTest {

    private static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");

    private User user;

    @BeforeEach
    public void setUp() {
        user = new CustomerBuilder().setFirstName("Luigi").setLastName("Verdi")
                .setPhoneNumber("1234567890")
                .build();
    }

    @Test
    public void builder_WithoutFirstName_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new CustomerBuilder()
                .setLastName("Verdi").setPhoneNumber("1234567890").build());
    }

    @Test
    public void builder_WithoutLastName_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new CustomerBuilder()
                .setFirstName("Luigi").setPhoneNumber("1234567890").build());
    }

    @Test
    public void builder_WithoutPhoneNumber_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new CustomerBuilder()
                .setFirstName("Luigi").setLastName("Verdi").build());
    }

    @Test
    public void activate_SetsIsActiveToTrue() {
        user.activate();

        assertTrue(user.isActive());
    }

    @Test
    public void deactivate_SetsIsActiveToFalse() {
        user.activate();

        user.deactivate();

        assertFalse(user.isActive());
    }

    @Test
    public void acceptsTokenIssuedAt_NoInvalidation_ReturnsTrue() {
        assertTrue(user.acceptsTokenIssuedAt(NOW));
    }

    @Test
    public void acceptsTokenIssuedAt_IssuedAfterInvalidation_ReturnsTrue() {
        user.invalidateTokens(NOW);

        assertTrue(user.acceptsTokenIssuedAt(NOW.plus(1, ChronoUnit.MINUTES)));
    }

    @Test
    public void acceptsTokenIssuedAt_IssuedAtTheSameSecondAsInvalidation_ReturnsTrue() {
        user.invalidateTokens(NOW);

        assertTrue(user.acceptsTokenIssuedAt(NOW));
    }

    @Test
    public void acceptsTokenIssuedAt_IssuedBeforeInvalidation_ReturnsFalse() {
        user.invalidateTokens(NOW);

        assertFalse(user.acceptsTokenIssuedAt(NOW.minus(1, ChronoUnit.SECONDS)));
    }

    @Test
    public void invalidateTokens_TruncatesToSeconds() {
        // invalidation at 10:00:00.700
        // login at 10:00:00.900 produces a JWT with iat = 10:00:00
        user.invalidateTokens(NOW.plusMillis(700));

        assertTrue(user.acceptsTokenIssuedAt(NOW),
                "A token issued in the same second as the invalidation must be accepted");
    }

    @Test
    public void invalidateTokens_CalledTwice_UsesLatestInstant() {
        user.invalidateTokens(NOW);
        user.invalidateTokens(NOW.plus(1, ChronoUnit.HOURS));

        assertFalse(user.acceptsTokenIssuedAt(NOW.plus(30, ChronoUnit.MINUTES)));
    }
}