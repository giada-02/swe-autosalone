package com.autosalone.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phoneNumber,
        @Email(message = "must have a valid format") @Size(min = 1, message = "cannot be blank") String email,
        @Size(min = 1, message = "cannot be blank") String residenceCity,
        @Size(min = 1, message = "cannot be blank") String zipCode,
        @Size(min = 1, message = "cannot be blank") String fiscalCode,
        @Size(min = 1, message = "cannot be blank") String vatNumber) {
}
