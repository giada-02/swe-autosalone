package com.autosalone.enums;

import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.catalog.AccessoryPackage;
import com.autosalone.models.catalog.PurchasableItem;

public enum CatalogItemType {
    ACCESSORY {
        @Override
        public Class<? extends PurchasableItem> getEntityClass() {
            return Accessory.class;
        }
    },
    PACKAGE {
        @Override
        public Class<? extends PurchasableItem> getEntityClass() {
            return AccessoryPackage.class;
        }
    };

    public abstract Class<? extends PurchasableItem> getEntityClass();
}