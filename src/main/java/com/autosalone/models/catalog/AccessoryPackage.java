package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "accessory_packages")
public class AccessoryPackage extends PurchasableItem {

    @ManyToMany
    @JoinTable(name = "package_items", joinColumns = @JoinColumn(name = "package_id"), inverseJoinColumns = @JoinColumn(name = "item_id"))
    private List<PurchasableItem> items = new ArrayList<>();

    protected AccessoryPackage() {
    }

    public AccessoryPackage(String name, String description) {
        super(name, description);
    }

    @Override
    public BigDecimal getPrice() {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchasableItem item : items) {
            total = total.add(item.getPrice());
        }
        return total;
    }

    public List<PurchasableItem> getItems() {
        return items;
    }

    public void addItem(PurchasableItem item) {
        validateIsNotArchived();
        Objects.requireNonNull(item, "Item is required");

        item.validateIsNotArchived();
        validateAddSelf(item);
        validateItemAlreadyExists(item);

        this.items.add(item);
    }

    public void removeItem(PurchasableItem item) {
        validateIsNotArchived();
        Objects.requireNonNull(item, "Item is required");

        this.items.remove(item);
    }

    private void validateItemAlreadyExists(PurchasableItem item) {
        boolean alreadyExists = this.items.stream()
                .anyMatch(i -> i.getName().equalsIgnoreCase(item.getName()));
        if (alreadyExists)
            throw new IllegalArgumentException("This item is already in the package");
    }

    private void validateAddSelf(PurchasableItem item) {
        if (this == item)
            throw new IllegalArgumentException("Cannot add the package inside itself");
    }
}