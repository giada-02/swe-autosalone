package com.autosalone.dtos.auth;

import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
        @NotBlank String token,
        @NotBlank String newPassword) {
}