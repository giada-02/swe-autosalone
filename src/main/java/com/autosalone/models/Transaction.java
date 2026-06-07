package com.autosalone.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.autosalone.enums.TransactionType;

@Entity
@Table(name = "transactions")
public class Transaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount; // importo

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_contract_id")
    private Contract contract;

    protected Transaction() {
    }

    Transaction(String reason, BigDecimal amount, LocalDate date, TransactionType type) {
        validateAmount(amount);
        this.reason = reason;
        this.amount = amount;
        this.date = date;
        this.type = type;
    }

    Transaction(String reason, BigDecimal amount, LocalDate date, TransactionType type, Vehicle vehicle) {
        this(reason, amount, date, type);
        this.vehicle = vehicle;
    }

    Transaction(String reason, BigDecimal amount, LocalDate date, TransactionType type, Contract contract) {
        this(reason, amount, date, type);
        this.contract = contract;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The amount of the transaction must be > 0");
        }
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

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Contract getContract() {
        return contract;
    }
}
