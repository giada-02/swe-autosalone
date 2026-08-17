package com.autosalone.dtos.requests;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record SalesDocumentCreateRequest(
        @NotNull UUID vehicleId,
        @NotNull UUID customerId) {
}
