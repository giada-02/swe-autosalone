package com.autosalone.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record AccessoryPackageRequest(
        @NotBlank @Size(max = 255, message = "cannot exceed 255 characters") String name,
        @Size(max = 255, message = "cannot exceed 255 characters") String description,
        Set<UUID> purchasableItemIds) {
}