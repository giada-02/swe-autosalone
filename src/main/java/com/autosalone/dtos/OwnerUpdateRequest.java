package com.autosalone.dtos;

public record OwnerUpdateRequest(String firstName,
        String lastName,
        String phoneNumber,
        String email) {
}
