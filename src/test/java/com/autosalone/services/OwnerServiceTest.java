package com.autosalone.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.OwnerRequest;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Owner;
import com.autosalone.repositories.OwnerRepository;

@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    @Mock
    private OwnerRepository ownerRepository;

    @InjectMocks
    private OwnerService ownerService;

    private UUID ownerId;
    private Owner mockOwner;
    private OwnerRequest ownerRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        mockOwner = mock(Owner.class);

        ownerRequest = new OwnerRequest(
                "Mario",
                "Rossi",
                "3331234567",
                "mario.rossi@example.com");
    }

    // read

    @Test
    void getOwnerById_Success() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(mockOwner));
        Owner result = ownerService.getOwnerById(ownerId);
        assertNotNull(result);
        assertEquals(mockOwner, result);
    }

    @Test
    void getOwnerById_NotFound() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            ownerService.getOwnerById(ownerId);
        });
    }

    @Test
    void getOwners_Success() {
        when(ownerRepository.findOwners(true)).thenReturn(List.of(mockOwner));

        List<Owner> results = ownerService.getOwners(true);

        assertEquals(1, results.size());
        verify(ownerRepository).findOwners(true);
    }

    // write

    @Test
    void addOwner_Success() {
        when(ownerRepository.findByEmail(ownerRequest.email())).thenReturn(Optional.empty());

        ownerService.addOwner(ownerRequest);

        ArgumentCaptor<Owner> captor = ArgumentCaptor.forClass(Owner.class);
        verify(ownerRepository).save(captor.capture());

        Owner savedOwner = captor.getValue();
        assertEquals("Mario", savedOwner.getFirstName());
        assertEquals("Rossi", savedOwner.getLastName());
        assertEquals("mario.rossi@example.com", savedOwner.getEmail());
    }

    @Test
    void addOwner_FailsIfEmailAlreadyInUse() {
        when(ownerRepository.findByEmail(ownerRequest.email())).thenReturn(Optional.of(mockOwner));

        assertThrows(IllegalStateException.class, () -> {
            ownerService.addOwner(ownerRequest);
        });

        verify(ownerRepository, never()).save(any());
    }

    @Test
    void updateOwner_Success_SameEmail() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(mockOwner));
        when(mockOwner.getEmail()).thenReturn(ownerRequest.email());

        assertDoesNotThrow(() -> ownerService.updateOwner(ownerId, ownerRequest));

        verify(ownerRepository, never()).findByEmail(anyString());

        verify(mockOwner).setFirstName("Mario");
        verify(mockOwner).setLastName("Rossi");
        verify(ownerRepository).save(mockOwner);
    }

    @Test
    void updateOwner_Success_DifferentEmail() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(mockOwner));
        when(mockOwner.getEmail()).thenReturn("vecchia.email@example.com");

        when(ownerRepository.findByEmail(ownerRequest.email())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> ownerService.updateOwner(ownerId, ownerRequest));

        verify(ownerRepository).findByEmail(ownerRequest.email());

        verify(mockOwner).setEmail(ownerRequest.email());
        verify(ownerRepository).save(mockOwner);
    }

    @Test
    void updateOwner_FailsIfDifferentEmailAlreadyInUse() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(mockOwner));
        when(mockOwner.getEmail()).thenReturn("vecchia.email@example.com");

        Owner anotherOwner = mock(Owner.class);
        when(ownerRepository.findByEmail(ownerRequest.email())).thenReturn(Optional.of(anotherOwner));

        assertThrows(IllegalStateException.class, () -> {
            ownerService.updateOwner(ownerId, ownerRequest);
        });

        verify(ownerRepository, never()).save(any());
    }
}