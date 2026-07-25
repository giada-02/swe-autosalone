package com.autosalone.services;

import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.OwnerRequest;
import com.autosalone.models.Owner;
import com.autosalone.repositories.OwnerRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OwnerService {

    @Inject
    private OwnerRepository ownerRepository;

    // read

    public Owner getOwnerById(UUID id) {
        return ownerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found of id: " + id));
    }

    public List<Owner> getOwners(Boolean isActive) {
        return ownerRepository.findOwners(isActive);
    }

    // write

    @Transactional
    public UUID addOwner(OwnerRequest request) {

        if (ownerRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("This email is already in use");
        }

        Owner newOwner = new Owner.OwnerBuilder()
                .setFirstName(request.firstName())
                .setLastName(request.lastName())
                .setPhoneNumber(request.phoneNumber())
                .setEmail(request.email())
                .build();

        ownerRepository.save(newOwner);
        return newOwner.getId();
    }

    @Transactional
    public void updateOwner(UUID ownerId, OwnerRequest request) {
        Owner owner = getOwnerById(ownerId);

        if (!owner.getEmail().equals(request.email())) {
            ownerRepository.findByEmail(request.email()).ifPresent(existing -> {
                throw new IllegalStateException("This email is already in use");
            });
        }

        owner.setFirstName(request.firstName());
        owner.setLastName(request.lastName());
        owner.setPhoneNumber(request.phoneNumber());
        owner.setEmail(request.email());

        ownerRepository.save(owner);
    }
}
