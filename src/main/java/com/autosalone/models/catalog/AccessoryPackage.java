package com.autosalone.models.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    public void addItem(PurchasableItem item) {
        validateIsNotArchived();
        this.items.add(item);
    }

    public void removeItem(PurchasableItem item) {
        validateIsNotArchived();
        this.items.remove(item);
    }

}