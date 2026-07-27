package com.autosalone.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phoneNumber,
        @Email String email,
        String residenceCity,
        String zipCode,
        String fiscalCode,
        String vatNumber) {
}
