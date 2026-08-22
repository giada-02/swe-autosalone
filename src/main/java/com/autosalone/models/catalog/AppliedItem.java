package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

import com.autosalone.models.catalog.visitors.ActiveItemValidatorVisitor;

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
        ActiveItemValidatorVisitor inspector = new ActiveItemValidatorVisitor();
        item.accept(inspector);

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
        Objects.requireNonNull(appliedPrice, "Applied price is required");
        if (this.appliedPrice != null && this.appliedPrice.compareTo(appliedPrice) == 0)
            return;

        if (appliedPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Applied price cannot be negative");
        }

        this.appliedPrice = appliedPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AppliedItem that = (AppliedItem) o;

        if (this.item == null || that.item == null)
            return false;
        return this.item.getId().equals(that.item.getId());
    }

    @Override
    public int hashCode() {
        return item != null && item.getId() != null ? item.getId().hashCode() : 0;
    }
}
