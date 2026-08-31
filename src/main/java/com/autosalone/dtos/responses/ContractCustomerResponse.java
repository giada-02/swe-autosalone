package com.autosalone.dtos.responses;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.autosalone.enums.DiscountType;
import com.autosalone.enums.ContractStatus;
import com.autosalone.models.Contract;
import com.autosalone.models.CustomerSnapshot;

public record ContractCustomerResponse(
        UUID id,
        String date,
        ContractStatus status,
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
        TransactionResponse deposit,
        UUID sourceQuotationId,
        String estimatedHandoverDate,
        String cancelationReason,
        CustomerSnapshot customerSnapshot,
        List<TransactionResponse> payments,
        BigDecimal totalPayment,
        BigDecimal remainingBalance) {

    public static ContractCustomerResponse fromEntity(Contract contract) {
        if (contract == null)
            return null;

        VehicleCustomerResponse vehicleResponse = contract.getVehicle() != null
                ? VehicleCustomerResponse.fromEntity(contract.getVehicle())
                : null;

        CustomerResponse customerResponse = contract.getCustomer() != null
                ? CustomerResponse.fromEntity(contract.getCustomer(), false)
                : null;

        List<AppliedItemResponse> itemsResponse = contract.getItems() != null
                ? contract.getItems().stream()
                        .map(AppliedItemResponse::fromEntity)
                        .toList()
                : List.of();

        List<TransactionResponse> paymentsReponse = contract.getPayments() != null
                ? contract.getPayments().stream()
                        .map(TransactionResponse::fromEntity)
                        .toList()
                : List.of();

        return new ContractCustomerResponse(
                contract.getId(),
                contract.getDate() != null ? contract.getDate().toString() : null,
                contract.getStatus(),
                vehicleResponse,
                customerResponse,
                itemsResponse,
                contract.getAdditionalFees(),
                contract.getPublicNotes(),
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
                contract.getCustomerSnapshot(),
                paymentsReponse,
                contract.getTotalPayment(),
                contract.getRemainingBalance());
    }
}
