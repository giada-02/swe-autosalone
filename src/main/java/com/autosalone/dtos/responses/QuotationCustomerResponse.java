package com.autosalone.dtos.responses;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.autosalone.enums.DiscountType;
import com.autosalone.enums.ExpirationPolicy;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.models.Quotation;

public record QuotationCustomerResponse(
        UUID id,
        String date,
        QuotationStatus status,
        VehicleCustomerResponse vehicle,
        CustomerResponse customer,
        List<AppliedItemResponse> appliedItems,
        BigDecimal additionalFees,
        String publicNotes,
        BigDecimal vehicleSellingPriceSnapshot,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal finalPrice,
        String expirationDate,
        ExpirationPolicy expirationPolicy) {

    public static QuotationCustomerResponse fromEntity(Quotation quotation) {
        if (quotation == null)
            return null;

        VehicleCustomerResponse vehicleResponse = quotation.getVehicle() != null
                ? VehicleCustomerResponse.fromEntity(quotation.getVehicle())
                : null;

        CustomerResponse customerResponse = quotation.getCustomer() != null
                ? CustomerResponse.fromEntity(quotation.getCustomer(), false)
                : null;

        List<AppliedItemResponse> itemsResponse = quotation.getItems() != null
                ? quotation.getItems().stream()
                        .map(AppliedItemResponse::fromEntity)
                        .toList()
                : List.of();

        return new QuotationCustomerResponse(
                quotation.getId(),
                quotation.getDate() != null ? quotation.getDate().toString() : null,
                quotation.getStatus(),
                vehicleResponse,
                customerResponse,
                itemsResponse,
                quotation.getAdditionalFees(),
                quotation.getPublicNotes(),
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
