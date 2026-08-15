package com.autosalone.dtos.responses;

import java.util.UUID;

import com.autosalone.models.Owner;

public record OwnerResponse(
        UUID id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        boolean isActive,
        boolean hasActiveInvitation) {

    public static OwnerResponse fromEntity(Owner owner, boolean hasActiveInvitation) {
        if (owner == null)
            return null;

        return new OwnerResponse(
                owner.getId(),
                owner.getFirstName(),
                owner.getLastName(),
                owner.getPhoneNumber(),
                owner.getEmail(),
                owner.isActive(),
                hasActiveInvitation);
    }
}