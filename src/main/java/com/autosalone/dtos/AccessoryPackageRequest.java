package com.autosalone.dtos;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;

public record AccessoryPackageRequest(
        @NotBlank String name,
        String description,
        Set<UUID> purchasableItemIds) {
}