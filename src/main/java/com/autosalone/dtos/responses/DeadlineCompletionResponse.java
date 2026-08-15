package com.autosalone.dtos.responses;

public record DeadlineCompletionResponse(
        DeadlineResponse completedDeadline,
        DeadlineResponse nextDeadline) {
}
