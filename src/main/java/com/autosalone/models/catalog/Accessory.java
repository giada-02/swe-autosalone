package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;

import com.autosalone.models.catalog.visitors.PurchasableItemVisitor;

@Entity
@Table(name = "accessories")
public class Accessory extends PurchasableItem {

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    protected Accessory() {
    }

    public Accessory(String name, String description, BigDecimal basePrice) {
        super(name, description);
        this.basePrice = validateBasePrice(basePrice);
    }

    @Override
    public BigDecimal getPrice() {
        return basePrice;
    }

    @Override
    public void accept(PurchasableItemVisitor visitor) {
        visitor.visit(this);
    }

    public void setBasePrice(BigDecimal basePrice) {
        validateBasePrice(basePrice);
        if (this.basePrice != null && this.basePrice.compareTo(basePrice) == 0)
            return;
        validateIsNotArchived();

        this.basePrice = basePrice;
    }

    // validation
    private static BigDecimal validateBasePrice(BigDecimal basePrice) {
        if (basePrice == null)
            throw new IllegalArgumentException("Base price is required");
        if (basePrice.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Base price cannot be negative");
        return basePrice;
    }
}
