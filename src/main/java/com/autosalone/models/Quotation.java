package com.autosalone.models;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Objects;

import com.autosalone.enums.ExpirationPolicy;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.enums.VehicleCondition;

@Entity
@Table(name = "quotations")
public class Quotation extends SalesDocument {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuotationStatus status;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiration_policy")
    private ExpirationPolicy expirationPolicy;

    protected Quotation() {
        super();
    }

    public Quotation(Vehicle vehicle, Customer customer) {
        super(vehicle, customer);
        this.status = QuotationStatus.DRAFT;
    }

    /// Cloning copy constructor
    public Quotation(Quotation original) {
        super(original, true);
        this.status = QuotationStatus.DRAFT;
        this.expirationDate = null;
        this.expirationPolicy = null;
    }

    // getters
    public QuotationStatus getStatus() {
        if (this.status == QuotationStatus.ISSUED && isPastExpiration()) // lazy expired status evaluation
            this.status = QuotationStatus.EXPIRED;
        return status;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public ExpirationPolicy getExpirationPolicy() {
        return expirationPolicy;
    }

    public boolean isPastExpiration() {
        if (this.expirationDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(this.expirationDate);
    }

    // setters
    @Override
    public void setDate(LocalDate date) {
        super.setDate(date);
        if (this.expirationPolicy != null) {
            this.expirationDate = this.expirationPolicy.calculateExpirationDate(date, this.expirationDate);
        }
    }

    public void issue() {
        if (this.status != QuotationStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only a DRAFT quotation can be issued (Current status: " + this.status + ")");
        }

        if (this.isArchived()) {
            throw new IllegalStateException("Cannot issue an ARCHIVED quotation");
        }

        this.getVehicle().validateVehicleStatusForDocument("Cannot issue the quotation");
        if (this.getVehicle().getCondition() == VehicleCondition.SECONDHAND) {
            validateSecondhandVehicle("Cannot issue the quotation");
        }

        if (this.expirationDate == null) {
            throw new IllegalStateException("Cannot issue a quotation without an expiration date");
        }
        if (isPastExpiration()) {
            throw new IllegalStateException("Cannot issue a quotation with an expiration date already in the past");
        }

        this.status = QuotationStatus.ISSUED;
    }

    public void accept() {
        if (this.status != QuotationStatus.ISSUED) {
            throw new IllegalStateException("Only an ISSUED quotation can be accepted");
        }
        this.status = QuotationStatus.ACCEPTED;
    }

    public void expire() {
        if (this.status != QuotationStatus.ISSUED) {
            throw new IllegalStateException("Only an ISSUED quotation can expire");
        }
        this.status = QuotationStatus.EXPIRED;
    }

    public void voidDocument() {
        if (this.status != QuotationStatus.ISSUED && this.status != QuotationStatus.DRAFT) {
            throw new IllegalStateException("Only an ISSUED or DRAFT quotation can be voided");
        }
        this.status = QuotationStatus.VOIDED;
    }

    public void updateExpiration(ExpirationPolicy expirationPolicy, LocalDate expirationDate) {
        validateIsEditable();
        Objects.requireNonNull(expirationPolicy, "Expiration policy is required");
        this.expirationDate = expirationPolicy.calculateExpirationDate(this.getDate(), expirationDate);
        this.expirationPolicy = expirationPolicy;
    }

    public void updateExpiration(ExpirationPolicy expirationPolicy) {
        if (expirationPolicy == ExpirationPolicy.CUSTOM)
            throw new IllegalArgumentException(
                    "Cannot apply CUSTOM policy without providing an explicit expiration date");
        updateExpiration(expirationPolicy, null);
    }

    public void updateExpiration(LocalDate expirationDate) {
        updateExpiration(ExpirationPolicy.CUSTOM, expirationDate);

    }

    @Override
    protected void validateIsEditable() {
        if (this.status != QuotationStatus.DRAFT)
            throw new IllegalStateException("Cannot edit quotation with status " + this.status);
    }

    @Override
    protected void validateIsArchivable() {
        if (this.status == QuotationStatus.ISSUED)
            throw new IllegalStateException("Cannot archive ISSUED quotation");
    }

    @Override
    protected boolean isDraft() {
        return this.status == QuotationStatus.DRAFT;
    }
}
