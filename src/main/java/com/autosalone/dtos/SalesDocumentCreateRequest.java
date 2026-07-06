package com.autosalone.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record SalesDocumentCreateRequest(
        @NotNull UUID vehicleId,
        @NotNull UUID customerId) {
}
