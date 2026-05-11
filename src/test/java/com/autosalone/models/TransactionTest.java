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
    public void constructor_NullAmount_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("Spesa", null, LocalDate.now(), TransactionType.OUT);
        });
    }

    @Test
    public void constructor_ZeroAmount_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("Spesa", BigDecimal.ZERO, LocalDate.now(), TransactionType.OUT);
        });
    }

    @Test
    public void constructor_NegativeAmount_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Transaction("Spesa", new BigDecimal("-1000.00"), LocalDate.now(), TransactionType.OUT);
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
