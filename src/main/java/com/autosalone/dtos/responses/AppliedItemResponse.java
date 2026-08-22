package com.autosalone.dtos.responses;

import java.math.BigDecimal;

import com.autosalone.models.catalog.AppliedItem;

public record AppliedItemResponse(
        CatalogItemResponse item,
        BigDecimal appliedPrice) {

    public static AppliedItemResponse fromEntity(AppliedItem appliedItem) {
        if (appliedItem == null || appliedItem.getItem() == null) {
            return null;
        }

        return new AppliedItemResponse(
                CatalogItemResponse.fromEntity(appliedItem.getItem()),
                appliedItem.getAppliedPrice());
    }
}