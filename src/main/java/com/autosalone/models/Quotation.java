package com.autosalone.models;

import java.time.LocalDate;

import com.autosalone.enums.ExpirationPolicy;
import com.autosalone.enums.QuotationStatus;

public class Quotation extends SalesDocument {
    private QuotationStatus status;
    private LocalDate expirationDate;
    private ExpirationPolicy expirationPolicy;
    private String publicNotes;
    private String internalNotes;

    public Quotation(Vehicle vehicle, Customer customer) {
        super(vehicle, customer);
        this.status = QuotationStatus.DRAFT;
    }

    // copy constructor for cloning
    public Quotation(Quotation original) {
        super(original);
        this.status = QuotationStatus.DRAFT;
        this.expirationDate = null;
        this.publicNotes = original.publicNotes;
        this.internalNotes = original.internalNotes;
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

    public boolean isPastExpiration() {
        if (this.expirationDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(this.expirationDate);
    }

    public String getPublicNotes() {
        return publicNotes;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    // setters
    @Override
    public void setDate(LocalDate date) {
        super.setDate(date);
        recalculateExpiration();
    }

    public void setStatus(QuotationStatus status) {
        if (this.status == status)
            return;
        if (!this.status.canTransitionTo(status)) {
            throw new IllegalStateException("Cannot transition quotation from status " + this.status + " to " + status);
        }
        if (status == QuotationStatus.ISSUED) {
            if (this.expirationDate == null) {
                throw new IllegalStateException("Cannot issue a quotation without an expiration date");
            }
            if (isPastExpiration()) {
                throw new IllegalStateException("Cannot issue a quotation with an expiration date already in the past");
            }
        }
        this.status = status;
        if (status == QuotationStatus.ISSUED)
            this.setVisibleToCustomer(true);
    }

    public void setExpirationDate(LocalDate expirationDate) {
        LocalDate today = LocalDate.now();
        if (expirationDate.isBefore(today))
            throw new IllegalArgumentException("Cannot set the expiration date in the past");
        if (!this.status.isEditable() && expirationDate.isBefore(this.expirationDate))
            throw new IllegalArgumentException(
                    "Cannot set a new expiration date earlier than the current expiration date for quotation with status "
                            + this.status);
        this.expirationPolicy = ExpirationPolicy.CUSTOM;
        this.expirationDate = expirationDate;
    }

    public void setExpirationPolicy10Days() {
        validateIsEditable();
        this.expirationPolicy = ExpirationPolicy.TEN_DAYS;
        recalculateExpiration();
    }

    public void setExpirationPolicyEndOfMonth() {
        validateIsEditable();
        this.expirationPolicy = ExpirationPolicy.END_OF_MONTH;
        recalculateExpiration();
    }

    public void setPublicNotes(String publicNotes) {
        validateIsEditable();
        this.publicNotes = publicNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }

    @Override
    protected void validateIsEditable() {
        if (!this.status.isEditable())
            throw new IllegalStateException("Cannot edit quotation with status " + this.status);
    }

    @Override
    protected void validateIsArchivable() {
        if (!this.status.isArchivable())
            throw new IllegalStateException("Cannot archive quotation with status " + this.status);
    }

    @Override
    protected void validateVisibilityChange(boolean isVisibleToCustomer) {
        if (isVisibleToCustomer && !this.status.canBeVisibleToCustomer())
            throw new IllegalStateException(
                    "Cannot set quotation with status " + this.status + " as visible to the customer");
    }

    private void recalculateExpiration() {
        if (this.expirationPolicy == null || this.expirationPolicy == ExpirationPolicy.CUSTOM) {
            return;
        }
        this.expirationDate = this.expirationPolicy.calculateExpirationDate(this.getDate());
    }
}
