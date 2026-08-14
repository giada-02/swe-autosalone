package com.autosalone.dtos.responses;

import java.util.UUID;

import com.autosalone.models.User;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        boolean isActive) {

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.isActive());
    }
}