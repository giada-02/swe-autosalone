package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.autosalone.models.AuditableEntity;

@Entity
@Table(name = "purchasable_items")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class PurchasableItem extends AuditableEntity {

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
        Objects.requireNonNull(name, "Name is required");
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
        validateIsNotArchived();
        Objects.requireNonNull(name, "Name is required");
        this.name = name;
    }

    public void setDescription(String description) {
        validateIsNotArchived();
        this.description = description;
    }

    public void archive() {
        if (this.isArchived)
            return;
        this.isArchived = true;
    }

    public void unarchive() {
        if (!this.isArchived)
            return;
        this.isArchived = false;
    }

    public void validateIsNotArchived() {
        if (this.isArchived)
            throw new IllegalStateException("Cannot edit nor add an archived item");
    }
}