package com.autosalone.models.catalog.visitors;

import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.catalog.AccessoryPackage;
import com.autosalone.models.catalog.PurchasableItem;

public class ActiveItemValidatorVisitor implements PurchasableItemVisitor {

    @Override
    public void visit(Accessory accessory) {
        if (accessory.isArchived()) {
            throw new IllegalArgumentException(
                    "Cannot add this item: the accessory '" + accessory.getName() + "' is archived");
        }
    }

    @Override
    public void visit(AccessoryPackage accessoryPackage) {
        if (accessoryPackage.isArchived()) {
            throw new IllegalArgumentException(
                    "Cannot add this item: the package '" + accessoryPackage.getName() + "' is archived");
        }

        for (PurchasableItem child : accessoryPackage.getItems()) {
            child.accept(this);
        }
    }
}