package com.autosalone.dtos.responses;

import java.util.List;
import java.util.UUID;

import com.autosalone.enums.CatalogItemType;
import com.autosalone.models.catalog.AccessoryPackage;
import com.autosalone.models.catalog.PurchasableItem;

public record CatalogAppliedItemResponse(
        UUID id,
        CatalogItemType type,
        String name,
        String description,
        List<CatalogAppliedItemResponse> subItems) {

    public static CatalogAppliedItemResponse fromEntity(PurchasableItem item) {
        if (item == null)
            return null;

        CatalogItemType itemType = (item instanceof AccessoryPackage) ? CatalogItemType.PACKAGE
                : CatalogItemType.ACCESSORY;

        List<CatalogAppliedItemResponse> subItems = null;
        if (item instanceof AccessoryPackage accessoryPackage) {
            subItems = accessoryPackage.getItems()
                    .stream().map(CatalogAppliedItemResponse::fromEntity).toList();
        }

        return new CatalogAppliedItemResponse(
                item.getId(),
                itemType,
                item.getName(),
                item.getDescription(),
                subItems);
    }
}
