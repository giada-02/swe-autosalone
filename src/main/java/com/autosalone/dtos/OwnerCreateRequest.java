package com.autosalone.dtos;

import jakarta.validation.constraints.NotBlank;

public record OwnerCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phoneNumber,
        String email) {
}
