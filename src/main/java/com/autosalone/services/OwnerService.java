package com.autosalone.services;

import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.requests.OwnerRequest;
import com.autosalone.dtos.responses.OwnerListResponse;
import com.autosalone.dtos.responses.OwnerResponse;
import com.autosalone.enums.TokenType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Owner;
import com.autosalone.repositories.AuthTokenRepository;
import com.autosalone.repositories.OwnerRepository;
import com.autosalone.repositories.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OwnerService {

    @Inject
    private OwnerRepository ownerRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private AuthTokenRepository authTokenRepository;

    // read

    public Owner getOwnerById(UUID id) {
        return ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found of id: " + id));
    }

    private boolean hasActiveInvitation(Owner owner) {
        return authTokenRepository
                .findByUserAndType(owner, TokenType.REGISTRATION)
                .map(token -> !token.isExpired())
                .orElse(false);
    }

    public OwnerResponse getOwnerResponseById(UUID id) {
        Owner owner = getOwnerById(id);
        boolean hasActiveInvitation = hasActiveInvitation(owner);
        return OwnerResponse.fromEntity(owner, hasActiveInvitation);
    }

    public List<OwnerListResponse> getOwners(Boolean isActive) {
        return ownerRepository.findOwners(isActive).stream()
                .map(OwnerListResponse::fromEntity)
                .toList();
    }

    // write

    @Transactional
    public OwnerResponse addOwner(OwnerRequest request) {

        if (request.email() != null && userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("This email is already in use");
        }

        Owner owner = new Owner.OwnerBuilder()
                .setFirstName(request.firstName())
                .setLastName(request.lastName())
                .setPhoneNumber(request.phoneNumber())
                .setEmail(request.email())
                .build();

        ownerRepository.save(owner);
        return OwnerResponse.fromEntity(owner, false);
    }

    @Transactional
    public OwnerResponse updateOwner(UUID id, OwnerRequest request) {
        Owner owner = getOwnerById(id);
        boolean hasActiveInvitation = hasActiveInvitation(owner);

        if (request.email() == null && (owner.isActive() || owner.getPassword() != null)) {
            throw new IllegalStateException("Cannot remove the email, the user is active");
        }

        if (owner.getEmail() != null && request.email() != null && !request.email().equals(owner.getEmail())) {
            userRepository.findByEmail(request.email()).ifPresent(existing -> {
                throw new IllegalStateException("This email is already in use");
            });
        }

        owner.setFirstName(request.firstName());
        owner.setLastName(request.lastName());
        owner.setPhoneNumber(request.phoneNumber());
        owner.setEmail(request.email());

        ownerRepository.save(owner);
        return OwnerResponse.fromEntity(owner, hasActiveInvitation);
    }
}
