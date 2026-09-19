package com.autosalone.dtos.responses;

public record QuotationCleanupResponse(
        int expiredQuotations,
        int voidedContracts,
        int freedVehicles
) {}
