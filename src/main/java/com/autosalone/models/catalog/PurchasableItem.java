package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.autosalone.models.AuditableEntity;
import com.autosalone.models.catalog.visitors.PurchasableItemVisitor;
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
        this.name = requireName(name);
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

    public abstract void accept(PurchasableItemVisitor visitor);

    // setters
    public void setName(String name) {
        String normalizedName = requireName(name);
        if (Objects.equals(this.name, normalizedName))
            return;

        validateIsNotArchived();
        this.name = normalizedName;
    }

    public void setDescription(String description) {
        String normalizedDescription = Utils.sanitizeText(description);
        if (Objects.equals(this.description, normalizedDescription))
            return;

        validateIsNotArchived();
        this.description = normalizedDescription;
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

    // validation and normalization
    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        return name.trim();
    }
}