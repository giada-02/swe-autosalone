package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "accessories")
public class Accessory extends PurchasableItem {

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    protected Accessory() {
    }

    public Accessory(String name, String description, BigDecimal basePrice) {
        super(name, description);
        validateBasePrice(basePrice);
        this.basePrice = basePrice;
    }

    @Override
    public BigDecimal getPrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        validateIsNotArchived();
        validateBasePrice(basePrice);
        this.basePrice = basePrice;
    }

    private void validateBasePrice(BigDecimal basePrice) {
        Objects.requireNonNull(basePrice, "Base price is required");
        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price cannot be negative");
        }
    }
}
