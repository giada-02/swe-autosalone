package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.autosalone.enums.TransactionType;

public class TransactionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);

    @Test
    public void constructor_NullReason_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            new Transaction(null, new BigDecimal("100.00"), TODAY, TransactionType.OUT);
        });
    }

    @Test
    public void constructor_NullAmount_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            new Transaction("Guadagno", null, TODAY, TransactionType.IN);
        });
    }

    @Test
    public void constructor_NullDate_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            new Transaction("Guadagno", new BigDecimal("100.00"), null, TransactionType.IN);
        });
    }

    @Test
    public void constructor_NullType_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            new Transaction("Spesa", new BigDecimal("100.00"), TODAY, null);
        });
    }

    @Test
    public void constructor_ZeroAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("Spesa", BigDecimal.ZERO, TODAY, TransactionType.OUT);
        });
    }

    @Test
    public void constructor_NegativeAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("Spesa", new BigDecimal("-100.00"), TODAY, TransactionType.OUT);
        });
    }

    @Test
    public void constructor_ValidData_CreatesTransaction() {
        Transaction transaction = new Transaction("Spesa", new BigDecimal("500.00"), TODAY, TransactionType.OUT);

        assertEquals("Spesa", transaction.getReason());
        assertEquals(0, transaction.getAmount().compareTo(new BigDecimal("500.00")));
        assertTrue(TODAY.equals(transaction.getDate()));
        assertEquals(TransactionType.OUT, transaction.getType());
    }
}
