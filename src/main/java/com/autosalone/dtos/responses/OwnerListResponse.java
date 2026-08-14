package com.autosalone.dtos.responses;

import java.util.UUID;

import com.autosalone.models.Owner;

public record OwnerListResponse(
        UUID id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        boolean isActive) {

    public static OwnerListResponse fromEntity(Owner owner) {
        return new OwnerListResponse(
                owner.getId(),
                owner.getFirstName(),
                owner.getLastName(),
                owner.getPhoneNumber(),
                owner.getEmail(),
                owner.isActive());
    }
}