package com.autosalone.models.catalog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AccessoryPackage implements PurchasableItem {
    private String packageName;
    private List<PurchasableItem> items;

    public AccessoryPackage(String packageName) {
        this.packageName = packageName;
        this.items = new ArrayList<>();
    }

    // getters
    @Override
    public String getName() {
        return packageName;
    }

    @Override
    public BigDecimal getPrice() {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchasableItem item : items) {
            total = total.add(item.getPrice());
        }
        return total;
    }

    // setters
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void addItem(PurchasableItem item) {
        this.items.add(item);
    }

    public void removeItem(PurchasableItem item) {
        this.items.remove(item);
    }

}
