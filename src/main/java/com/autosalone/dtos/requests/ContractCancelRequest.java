package com.autosalone.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContractCancelRequest(
        @NotBlank @Size(max = 500, message = "cannot exceed 500 characters") String reason) {
}