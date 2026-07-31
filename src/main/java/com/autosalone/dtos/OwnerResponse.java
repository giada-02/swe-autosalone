package com.autosalone.dtos;

import java.util.UUID;

import com.autosalone.models.Owner;

public record OwnerResponse(
        UUID id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        boolean isActive) {

    public static OwnerResponse fromEntity(Owner owner) {
        return new OwnerResponse(
                owner.getId(),
                owner.getFirstName(),
                owner.getLastName(),
                owner.getPhoneNumber(),
                owner.getEmail(),
                owner.isActive());
    }
}