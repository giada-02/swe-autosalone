package com.autosalone.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.autosalone.enums.ContractStatus;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.enums.TransactionType;

@Entity
@Table(name = "contracts")
public class Contract extends SalesDocument {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;

    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "deposit_transaction_id", referencedColumnName = "id")
    private Transaction deposit; // caparra

    @OneToMany(mappedBy = "contract", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<Transaction> payments = new ArrayList<>(); // acconti

    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "source_quotation_id", referencedColumnName = "id")
    private Quotation quotationReference;

    @Column(name = "estimated_handover_date")
    private LocalDate estimatedHandoverDate;

    @Column(name = "cancelation_reason", columnDefinition = "TEXT")
    private String cancelationReason;

    protected Contract() {
        super();
    }

    public Contract(Vehicle vehicle, Customer customer) {
        super(vehicle, customer);
        this.status = ContractStatus.DRAFT;
    }

    /// Conversion Constructor: Turns a quotation into a contract
    public Contract(Quotation source) {
        super(source);
        if (source.isArchived()) {
            throw new IllegalStateException("Cannot create a contract from an ARCHIVED quotation.");
        }
        if (source.getStatus() != QuotationStatus.ISSUED) {
            throw new IllegalStateException("A contract can only be created from an ISSUED quotation (Current status: "
                    + source.getStatus() + ")");
        }

        this.status = ContractStatus.DRAFT;
        this.quotationReference = source;
    }

    // getters
    public ContractStatus getStatus() {
        return status;
    }

    public Transaction getDeposit() {
        return deposit;
    }

    public List<Transaction> getPayments() {
        return payments;
    }

    public Quotation getQuotationReference() {
        return quotationReference;
    }

    public LocalDate getEstimatedHandoverDate() {
        return estimatedHandoverDate;
    }

    public String getCancelationReason() {
        return cancelationReason;
    }

    public BigDecimal getTotalPayment() {
        BigDecimal total = BigDecimal.ZERO;
        if (this.deposit != null) {
            total = total.add(this.deposit.getAmount());
        }
        for (Transaction payment : this.payments) {
            total = payment.getType() == TransactionType.IN
                    ? total.add(payment.getAmount())
                    : total.subtract(payment.getAmount());
        }
        return total;
    }

    public BigDecimal getRemainingBalance() { // prezzo finale - (caparra + acconti)
        return this.getFinalPrice().subtract(getTotalPayment());
    }

    // setters
    @Override
    public void setVehicle(Vehicle vehicle) {
        if (this.quotationReference != null) {
            throw new IllegalStateException(
                    "Cannot change vehicle: this contract is tied to a pre-existing quotation");
        }
        super.setVehicle(vehicle);
    }

    @Override
    public void setCustomer(Customer customer) {
        if (this.quotationReference != null) {
            throw new IllegalStateException(
                    "Cannot change customer: this contract is tied to a pre-existing quotation");
        }
        super.setCustomer(customer);
    }

    public void confirm(Transaction depositTransaction) {
        if (this.status != ContractStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT contract can be confirmed");
        }

        if (depositTransaction == null) {
            throw new IllegalStateException("Cannot confirm a contract without providing a deposit transaction");
        }

        if (depositTransaction.getType() != TransactionType.IN) {
            throw new IllegalArgumentException("Deposit must be an IN transaction");
        }

        this.deposit = depositTransaction;
        this.status = ContractStatus.CONFIRMED;
    }

    public void complete() {
        if (this.status != ContractStatus.CONFIRMED) {
            throw new IllegalStateException("Only a CONFIRMED contract can be completed");
        }

        if (this.getRemainingBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException(
                    "Cannot complete contract: balance is not zero. Remaining to pay: " + this.getRemainingBalance());
        }

        this.status = ContractStatus.COMPLETED;
    }

    public void cancel(String reason) {
        if (this.status != ContractStatus.CONFIRMED) {
            throw new IllegalStateException("Only a CONFIRMED contract can be canceled");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A cancelation reason must be provided");
        }

        this.cancelationReason = reason;
        this.status = ContractStatus.CANCELED;
    }

    public void registerPayment(Transaction payment) {
        if (this.status != ContractStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot accept payments for a contract in status " + this.status);
        }
        if (payment.getType() != TransactionType.IN) {
            throw new IllegalArgumentException("Payment must be a transaction of type IN");
        }
        if (payment.getAmount().compareTo(getRemainingBalance()) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds the remaining balance");
        }
        this.payments.add(payment);
    }

    public void registerRefund(Transaction refund) {
        if (this.status != ContractStatus.CONFIRMED && this.status != ContractStatus.CANCELED) {
            throw new IllegalStateException("Cannot process refunds for a contract in status " + this.status);
        }

        if (refund.getType() != TransactionType.OUT) {
            throw new IllegalArgumentException("Refund must be a transaction of type OUT");
        }

        if (refund.getAmount().compareTo(getTotalPayment()) > 0) {
            throw new IllegalArgumentException("Cannot refund more than what has been paid");
        }

        this.payments.add(refund);
    }

    @Override
    protected void validateIsEditable() {
        if (this.status != ContractStatus.DRAFT)
            throw new IllegalStateException("Cannot edit contract with status " + this.status);
    }

    @Override
    protected void validateIsArchivable() {
        if (this.status == ContractStatus.CONFIRMED)
            throw new IllegalStateException("Cannot archive a CONFIRMED contract");
    }

    @Override
    protected boolean isDraft() {
        return this.status == ContractStatus.DRAFT;
    }
}
