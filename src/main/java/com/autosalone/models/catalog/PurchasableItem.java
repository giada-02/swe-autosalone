package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchasable_items")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class PurchasableItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

    protected PurchasableItem() {
    }

    public PurchasableItem(String name, String description) {
        java.util.Objects.requireNonNull(name, "Name is required");
        this.name = name;
        this.description = description;
    }

    // getters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isArchived() {
        return isArchived;
    }

    protected abstract BigDecimal getPrice();

    // setters
    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void archive() {
        this.isArchived = true;
    }

    public void unarchive() {
        this.isArchived = false;
    }
}