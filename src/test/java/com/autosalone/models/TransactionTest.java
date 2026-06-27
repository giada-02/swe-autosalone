package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.autosalone.enums.TransactionType;

public class TransactionTest {
    @Test
    public void constructor_NullReason_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            new Transaction(null, new BigDecimal("100.00"), LocalDate.now(), TransactionType.OUT);
        });
    }

    @Test
    public void constructor_NullAmount_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            new Transaction("Guadagno", null, LocalDate.now(), TransactionType.IN);
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
            new Transaction("Spesa", new BigDecimal("100.00"), LocalDate.now(), null);
        });
    }

    @Test
    public void constructor_ZeroAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("Spesa", BigDecimal.ZERO, LocalDate.now(), TransactionType.OUT);
        });
    }

    @Test
    public void constructor_NegativeAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("Spesa", new BigDecimal("-100.00"), LocalDate.now(), TransactionType.OUT);
        });
    }

    @Test
    public void constructor_ValidData_CreatesTransaction() {
        Transaction transaction = new Transaction("Spesa", new BigDecimal("500.00"), LocalDate.now(),
                TransactionType.OUT);

        assertEquals("Spesa", transaction.getReason());
        assertTrue(new BigDecimal("500.00").equals(transaction.getAmount()));
        assertTrue(LocalDate.now().equals(transaction.getDate()));
        assertEquals(TransactionType.OUT, transaction.getType());
    }
}
