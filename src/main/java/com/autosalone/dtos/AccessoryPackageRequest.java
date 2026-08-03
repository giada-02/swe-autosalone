package com.autosalone.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record AccessoryPackageRequest(
        @NotBlank String name,
        @Size(min = 1, message = "cannot be blank") String description,
        Set<UUID> purchasableItemIds) {
}