package com.autosalone.models.catalog.visitors;

import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.catalog.AccessoryPackage;

public interface PurchasableItemVisitor {
    void visit(Accessory accessory);

    void visit(AccessoryPackage accessoryPackage);
}