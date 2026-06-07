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
        item.validateIsNotArchived();
        if (item instanceof AccessoryPackage && ((AccessoryPackage) item).getItems().isEmpty())
            throw new IllegalStateException("The package must contain at least an item");
        this.item = item;
        this.appliedPrice = item.getPrice();
    }

    public PurchasableItem getItem() {
        return item;
    }

    public BigDecimal getAppliedPrice() {
        return appliedPrice;
    }

    public void setAppliedPrice(BigDecimal appliedPrice) {
        Objects.requireNonNull(appliedPrice, "Applied price is required");
        if (appliedPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Applied price cannot be negative");
        }
        this.appliedPrice = appliedPrice;
    }
}
