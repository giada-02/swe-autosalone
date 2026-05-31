package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;

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
        if (appliedPrice == null || appliedPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Applied price cannot be null or negative");
        }
        this.appliedPrice = appliedPrice;
    }
}
