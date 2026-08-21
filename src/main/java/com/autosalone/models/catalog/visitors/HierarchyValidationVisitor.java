package com.autosalone.models.catalog.visitors;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.catalog.AccessoryPackage;
import com.autosalone.models.catalog.PurchasableItem;

public class HierarchyValidationVisitor implements PurchasableItemVisitor {

    private final Set<UUID> visitedIds = new HashSet<>();

    public HierarchyValidationVisitor(UUID rootPackageId) {
        if (rootPackageId != null) {
            visitedIds.add(rootPackageId);
        }
    }

    @Override
    public void visit(Accessory accessory) {
        if (!visitedIds.add(accessory.getId())) {
            throw new IllegalArgumentException(
                    "Duplicate detected: the accessory '" + accessory.getName() + "' is included multiple times.");
        }
    }

    @Override
    public void visit(AccessoryPackage accessoryPackage) {
        if (!visitedIds.add(accessoryPackage.getId())) {
            throw new IllegalArgumentException(
                    "Circular reference or duplicate package detected: '" + accessoryPackage.getName() + "'");
        }

        for (PurchasableItem item : accessoryPackage.getItems()) {
            item.accept(this);
        }
    }
}