package com.autosalone.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.autosalone.enums.TransactionType;

public class Transaction {
    private final UUID id;
    private final String reason;
    private final BigDecimal amount; // importo
    private final LocalDate date;
    private final TransactionType type;

    Transaction(String reason, BigDecimal amount, LocalDate date, TransactionType type) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The amount of the transaction must be > 0");
        }
        this.id = UUID.randomUUID();
        this.reason = reason;
        this.amount = amount;
        this.date = date;
        this.type = type;
    }

    // getters
    public UUID getId() {
        return id;
    }

    public String getReason() {
        return reason;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public TransactionType getType() {
        return type;
    }
}
