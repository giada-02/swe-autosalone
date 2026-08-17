package com.autosalone.dtos.responses;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.autosalone.enums.DiscountType;
import com.autosalone.enums.ExpirationPolicy;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.models.Quotation;
import com.autosalone.models.catalog.AppliedItem;

public record QuotationResponse(
        UUID id,
        String date,
        QuotationStatus status,
        VehicleResponse vehicle,
        CustomerResponse customer,
        List<AppliedItem> appliedItems,
        BigDecimal additionalFees,
        boolean isArchived,
        String publicNotes,
        String internalNotes,
        BigDecimal vehicleSellingPriceSnapshot,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal finalPrice,
        String expirationDate,
        ExpirationPolicy expirationPolicy) {

    public static QuotationResponse fromEntity(Quotation quotation) {
        if (quotation == null)
            return null;

        VehicleResponse vehicleResponse = quotation.getVehicle() != null
                ? VehicleResponse.fromEntity(quotation.getVehicle())
                : null;

        CustomerResponse customerResponse = quotation.getCustomer() != null
                ? CustomerResponse.fromEntity(quotation.getCustomer(), false)
                : null;

        return new QuotationResponse(
                quotation.getId(),
                quotation.getDate() != null ? quotation.getDate().toString() : null,
                quotation.getStatus(),
                vehicleResponse,
                customerResponse,
                quotation.getItems(),
                quotation.getAdditionalFees(),
                quotation.isArchived(),
                quotation.getPublicNotes(),
                quotation.getInternalNotes(),
                quotation.getVehicleSellingPriceSnapshot(),
                quotation.getDiscountType(),
                quotation.getDiscountValue(),
                quotation.getSubtotal(),
                quotation.getDiscountAmount(),
                quotation.getFinalPrice(),
                quotation.getExpirationDate() != null ? quotation.getExpirationDate().toString() : null,
                quotation.getExpirationPolicy());
    }
}
