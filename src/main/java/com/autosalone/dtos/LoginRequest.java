package com.autosalone.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email(message = "must have a valid format") String email,
        @NotBlank String password) {
}
