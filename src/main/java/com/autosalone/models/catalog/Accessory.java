package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

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
        Objects.requireNonNull(basePrice, "Base price is required");
        validateBasePriceValue(basePrice);

        this.basePrice = basePrice;
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
        Objects.requireNonNull(basePrice, "Base price is required");
        if (this.basePrice != null && this.basePrice.compareTo(basePrice) == 0)
            return;

        validateIsNotArchived();
        validateBasePriceValue(basePrice);

        this.basePrice = basePrice;
    }

    private void validateBasePriceValue(BigDecimal basePrice) {
        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price cannot be negative");
        }
    }
}
