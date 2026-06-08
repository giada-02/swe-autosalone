package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class AppliedItem {

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private PurchasableItem item;

    @Column(name = "applied_price", nullable = false)
    private BigDecimal appliedPrice;

    protected AppliedItem() {
    }

    public AppliedItem(PurchasableItem item) {
        if (item.isArchived())
            throw new IllegalArgumentException("The item must not be archived to add it in a sales document");
        this.item = item;
        this.appliedPrice = item.getPrice();
    }

    // copy constructor
    public AppliedItem(AppliedItem original) {
        this.item = original.getItem();
        this.appliedPrice = original.getAppliedPrice();
    }

    // cloning copy constructor
    public AppliedItem(AppliedItem original, boolean getCurrentItemBasePrice) {
        this.item = original.getItem();
        this.appliedPrice = getCurrentItemBasePrice ? original.getItem().getPrice() : original.getAppliedPrice();
    }

    public PurchasableItem getItem() {
        return item;
    }

    public BigDecimal getAppliedPrice() {
        return appliedPrice;
    }

    public void setAppliedPrice(BigDecimal appliedPrice) {
        validatePrice(appliedPrice);
        this.appliedPrice = appliedPrice;
    }

    private void validatePrice(BigDecimal price) {
        Objects.requireNonNull(price, "Applied price is required");
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Applied price cannot be negative");
        }
    }
}
