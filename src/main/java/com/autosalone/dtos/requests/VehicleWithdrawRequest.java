package com.autosalone.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleWithdrawRequest(
        @NotBlank @Size(max = 255, message = "cannot exceed 255 characters") String reason) {
}
