package com.autosalone.dtos;

import jakarta.validation.constraints.NotBlank;

public record CustomerCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phoneNumber,
        String email,
        String residenceCity,
        String zipCode,
        String fiscalCode,
        String vatNumber) {
}
