package com.autosalone.models.catalog;

import java.util.UUID;
import java.math.BigDecimal;

public class Accessory implements PurchasableItem {
    private final UUID id;
    private String name;
    private String description;
    private BigDecimal price;

    public Accessory(String name, String description, BigDecimal price) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.price = price;
    }

    // getters
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public BigDecimal getPrice() {
        return price;
    }

    // setters
    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
