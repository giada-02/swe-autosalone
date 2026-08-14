package com.autosalone.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContractCancelationRequest(
        @NotBlank @Size(max = 500, message = "cannot exceed 500 characters") String reason) {
}