package com.autosalone.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerRequest(
        @NotBlank @Size(max = 255, message = "cannot exceed 255 characters") String firstName,
        @NotBlank @Size(max = 255, message = "cannot exceed 255 characters") String lastName,
        @NotBlank @Size(min = 8, message = "cannot be under 8 characters") @Size(max = 20, message = "cannot exceed 20 characters") @Pattern(regexp = "^\\+?[0-9\\s\\-]+$", message = "contains invalid characters (only numbers, spaces, hyphens and + allowed)") String phoneNumber,
        @Email(message = "must have a valid format") @Size(max = 255, message = "cannot exceed 255 characters") String email) {
}
