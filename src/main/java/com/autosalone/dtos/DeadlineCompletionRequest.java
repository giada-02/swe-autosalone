package com.autosalone.dtos;

import jakarta.validation.constraints.Size;

public record DeadlineCompletionRequest(
        @Size(max = 1000, message = "cannot exceed 1000 characters") String notes) {
}