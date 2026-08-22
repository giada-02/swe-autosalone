package com.autosalone.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(
        @NotBlank @Size(min = 8, max = 64, message = "The password must be between 8 and 64 characters") String currentPassword,
        @NotBlank @Size(min = 8, max = 64, message = "The password must be between 8 and 64 characters") String newPassword) {
}