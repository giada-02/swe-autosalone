package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertThrows(IllegalArgumentException.class, () -> new CustomerBuilder()
                .setLastName("Verdi").setPhoneNumber("1234567890").build());
    }

    @Test
    public void builder_WithoutLastName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new CustomerBuilder()
                .setFirstName("Luigi").setPhoneNumber("1234567890").build());
    }

    @Test
    public void builder_WithoutPhoneNumber_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new CustomerBuilder()
                .setFirstName("Luigi").setLastName("Verdi").build());
    }

    @Test
    public void builder_NormalizesData_Success() {
        User messyUser = new CustomerBuilder()
                .setFirstName("  Mario ").setLastName("  Di  Rosso  ")
                .setPhoneNumber("  +39 123- 45 67 -  -890  ").setEmail("  Mario.DiRosso@Example.COM ").build();

        assertAll(
                () -> assertEquals("Mario", messyUser.getFirstName()),
                () -> assertEquals("Di Rosso", messyUser.getLastName()),
                () -> assertEquals("+391234567890", messyUser.getPhoneNumber()),
                () -> assertEquals("mario.dirosso@example.com", messyUser.getEmail()));
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
    public void setFirstName_Blank_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> user.setFirstName(" "));
        assertEquals("First name is required", exception.getMessage());
        assertEquals("Luigi", user.getFirstName());
    }

    @Test
    public void setFirstName_ToNull_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> user.setFirstName(null));
        assertEquals("First name is required", exception.getMessage());
        assertEquals("Luigi", user.getFirstName());
    }

    @Test
    public void setFirstName_NormalizesValue() {
        user.setFirstName("  Mario ");
        assertEquals("Mario", user.getFirstName());
    }

    @Test
    public void setLastName_Blank_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> user.setLastName(" "));
        assertEquals("Last name is required", exception.getMessage());
        assertEquals("Verdi", user.getLastName());
    }

    @Test
    public void setLastName_ToNull_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> user.setLastName(null));
        assertEquals("Last name is required", exception.getMessage());
        assertEquals("Verdi", user.getLastName());
    }

    @Test
    public void setLastName_NormalizesValue() {
        user.setLastName("  Di  Rosso  ");
        assertEquals("Di Rosso", user.getLastName());
    }

    @Test
    public void setPhoneNumber_Blank_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> user.setPhoneNumber(" "));
        assertEquals("Phone number is required", exception.getMessage());
        assertEquals("1234567890", user.getPhoneNumber());
    }

    @Test
    public void setPhoneNumber_ToNull_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> user.setPhoneNumber(null));
        assertEquals("Phone number is required", exception.getMessage());
        assertEquals("1234567890", user.getPhoneNumber());
    }

    @Test
    public void setPhoneNumber_NormalizesValue() {
        user.setPhoneNumber("  +39 123- 45 67 -  -890  ");
        assertEquals("+391234567890", user.getPhoneNumber());
    }

    @Test
    public void setEmail_NormalizesValue() {
        user.setEmail("  Mario.Rossi@Example.COM ");

        assertEquals("mario.rossi@example.com", user.getEmail());
    }

    @Test
    public void setEmail_ToNull_SetsNull() {
        user.setEmail("mario.rossi@example.com");

        user.setEmail(null);

        assertNull(user.getEmail());
    }

    @Test
    public void setEmail_Blank_SetsNull() {
        user.setEmail("mario.rossi@example.com");

        user.setEmail("  ");

        assertNull(user.getEmail());
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

    @Test
    public void hasSameEmail_SameEmails_ReturnsTrue() {
        user.setEmail("luigi.verdi@example.com");
        assertTrue(user.hasSameEmail("  Luigi.Verdi@Example.COM "));
    }

    @Test
    public void hasSameEmail_DifferentEmails_ReturnsFalse() {
        user.setEmail("luigi.verdi@example.com");
        assertFalse(user.hasSameEmail("mario.rossi@example.com"));
    }

    @Test
    public void hasSameEmail_NullEmails_ReturnsTrue() {
        assertTrue(user.hasSameEmail(null));
    }

    @Test
    public void normalizeEmail_TrimsAndLowercases() {
        assertEquals("mario.rossi@example.com", User.normalizeEmail("  Mario.Rossi@Example.COM "));
    }

    @Test
    public void normalizeEmail_Null_ReturnsNull() {
        assertNull(User.normalizeEmail(null));
    }

    @Test
    public void normalizeEmail_Blank_ReturnsNull() {
        assertNull(User.normalizeEmail(""));
        assertNull(User.normalizeEmail("   "));
    }
}