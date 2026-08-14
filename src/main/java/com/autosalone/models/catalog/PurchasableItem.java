package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.autosalone.models.AuditableEntity;
import com.autosalone.utils.Utils;

@Entity
@Table(name = "purchasable_items")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class PurchasableItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
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

    @PrePersist
    @PreUpdate
    protected void normalizeData() {
        this.name = this.name.trim();
        this.description = Utils.sanitizeText(description);
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

    public abstract BigDecimal getPrice();

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

    public void validateIsNotArchived() {
        if (this.isArchived)
            throw new IllegalStateException("No changes can be made to an archived purchasable item");
    }
}