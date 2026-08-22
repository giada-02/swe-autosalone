package com.autosalone.dtos.responses;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.autosalone.enums.DiscountType;
import com.autosalone.enums.ContractStatus;
import com.autosalone.models.Contract;
import com.autosalone.models.CustomerSnapshot;

public record ContractResponse(
        UUID id,
        String date,
        ContractStatus status,
        VehicleResponse vehicle,
        CustomerResponse customer,
        List<AppliedItemResponse> appliedItems,
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
        TransactionResponse deposit,
        UUID sourceQuotationId,
        String estimatedHandoverDate,
        String cancelationReason,
        CustomerSnapshot customerSnapshot) {

    public static ContractResponse fromEntity(Contract contract) {
        if (contract == null)
            return null;

        VehicleResponse vehicleResponse = contract.getVehicle() != null
                ? VehicleResponse.fromEntity(contract.getVehicle())
                : null;

        CustomerResponse customerResponse = contract.getCustomer() != null
                ? CustomerResponse.fromEntity(contract.getCustomer(), false)
                : null;

        List<AppliedItemResponse> itemsResponse = contract.getItems() != null
                ? contract.getItems().stream()
                        .map(AppliedItemResponse::fromEntity)
                        .toList()
                : List.of();

        return new ContractResponse(
                contract.getId(),
                contract.getDate() != null ? contract.getDate().toString() : null,
                contract.getStatus(),
                vehicleResponse,
                customerResponse,
                itemsResponse,
                contract.getAdditionalFees(),
                contract.isArchived(),
                contract.getPublicNotes(),
                contract.getInternalNotes(),
                contract.getVehicleSellingPriceSnapshot(),
                contract.getDiscountType(),
                contract.getDiscountValue(),
                contract.getSubtotal(),
                contract.getDiscountAmount(),
                contract.getFinalPrice(),
                TransactionResponse.fromEntity(contract.getDeposit()),
                contract.getQuotationReference() != null ? contract.getQuotationReference().getId() : null,
                contract.getEstimatedHandoverDate() != null ? contract.getEstimatedHandoverDate().toString() : null,
                contract.getCancelationReason(),
                contract.getCustomerSnapshot());
    }
}
