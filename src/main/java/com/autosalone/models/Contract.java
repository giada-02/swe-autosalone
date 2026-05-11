package com.autosalone.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.autosalone.enums.ContractStatus;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.enums.TransactionType;

public class Contract extends SalesDocument {
    private ContractStatus status;
    private Transaction deposit; // caparra
    private List<Transaction> payments; // acconti
    private Quotation quotationReference;

    public Contract(Vehicle vehicle, Customer customer) {
        super(vehicle, customer);
        this.status = ContractStatus.DRAFT;
        this.payments = new ArrayList<>();
    }

    /// Conversion Constructor: Turns a quotation into a contract
    public Contract(Quotation source) {
        super(source);
        if (source.isArchived()) {
            throw new IllegalStateException("Cannot create a contract from an ARCHIVED quotation");
        }
        QuotationStatus sourceStatus = source.getStatus();
        if (sourceStatus == QuotationStatus.EXPIRED || sourceStatus == QuotationStatus.DRAFT) {
            throw new IllegalStateException("Cannot create a contract from quotation with status " + sourceStatus);
        }
        this.status = ContractStatus.DRAFT;
        this.payments = new ArrayList<>();
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

    public BigDecimal getTotalPayment() {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction payment : this.payments) {
            total = payment.getType() == TransactionType.IN ? total.add(payment.getAmount())
                    : total.subtract(payment.getAmount());
        }
        return total;
    }

    public BigDecimal getRemainingBalance() {
        return this.getFinalPrice().subtract(getTotalPayment());
    }

    // setters
    public void setStatus(ContractStatus status) {
        if (this.status == status)
            return;
        if (!this.status.canTransitionTo(status))
            throw new IllegalStateException("Cannot transition contract from status " + this.status + " to " + status);
        this.status = status;
        if (status == ContractStatus.CONFIRMED)
            this.setVisibleToCustomer(true);
        if (status == ContractStatus.CANCELLED)
            this.setVisibleToCustomer(false);
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
        if (this.status != ContractStatus.CONFIRMED) {
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
        if (!this.status.isEditable())
            throw new IllegalStateException("Cannot edit contract with status " + this.status);
    }

    @Override
    protected void validateIsArchivable() {
        if (!this.status.isArchivable())
            throw new IllegalStateException("Cannot archive contract with status " + this.status);
    }

    @Override
    protected void validateVisibilityChange(boolean isVisibleToCustomer) {
        if (isVisibleToCustomer && !this.status.canBeVisibleToCustomer())
            throw new IllegalStateException(
                    "Cannot set contract with status " + this.status + " as visible to the customer");
    }
}
