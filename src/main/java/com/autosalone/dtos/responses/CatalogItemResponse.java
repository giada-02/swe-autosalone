package com.autosalone.dtos.responses;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.autosalone.enums.CatalogItemType;
import com.autosalone.models.catalog.AccessoryPackage;
import com.autosalone.models.catalog.PurchasableItem;

public record CatalogItemResponse(
        UUID id,
        CatalogItemType type,
        String name,
        String description,
        BigDecimal price,
        boolean isArchived,
        List<CatalogItemResponse> subItems) {

    public static CatalogItemResponse fromEntity(PurchasableItem item) {
        if (item == null)
            return null;

        CatalogItemType itemType = (item instanceof AccessoryPackage) ? CatalogItemType.PACKAGE
                : CatalogItemType.ACCESSORY;

        List<CatalogItemResponse> subItems = null;
        if (item instanceof AccessoryPackage accessoryPackage) {
            subItems = accessoryPackage.getItems()
                    .stream().map(CatalogItemResponse::fromEntity).toList();
        }

        return new CatalogItemResponse(
                item.getId(),
                itemType,
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.isArchived(),
                subItems);
    }
}
