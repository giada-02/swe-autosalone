package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.autosalone.models.catalog.visitors.PurchasableItemVisitor;

@Entity
@Table(name = "accessory_packages")
public class AccessoryPackage extends PurchasableItem {

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "package_items", joinColumns = @JoinColumn(name = "package_id"), inverseJoinColumns = @JoinColumn(name = "item_id"))
    private List<PurchasableItem> items = new ArrayList<>();

    protected AccessoryPackage() {
    }

    public AccessoryPackage(String name, String description) {
        super(name, description);
    }

    public AccessoryPackage(String name, String description, List<PurchasableItem> items) {
        super(name, description);
        if (items != null) {
            for (PurchasableItem item : items) {
                this.addItem(item);
            }
        }
    }

    @Override
    public BigDecimal getPrice() {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchasableItem item : items) {
            total = total.add(item.getPrice());
        }
        return total;
    }

    @Override
    public void accept(PurchasableItemVisitor visitor) {
        visitor.visit(this);
    }

    public List<PurchasableItem> getItems() {
        return items;
    }

    public void addItem(PurchasableItem item) {
        validateIsNotArchived();
        Objects.requireNonNull(item, "Item is required");

        validateAddSelf(item);
        item.validateIsNotArchived();

        this.items.add(item);
    }

    public void removeItem(PurchasableItem item) {
        validateIsNotArchived();
        Objects.requireNonNull(item, "Item is required");

        this.items.remove(item);
    }

    private void validateAddSelf(PurchasableItem item) {
        if (this == item)
            throw new IllegalArgumentException("Cannot add the package inside itself");
    }
}