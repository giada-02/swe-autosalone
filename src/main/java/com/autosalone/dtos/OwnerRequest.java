package com.autosalone.dtos;

import jakarta.validation.constraints.NotBlank;

public record OwnerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phoneNumber,
        String email) {
}
