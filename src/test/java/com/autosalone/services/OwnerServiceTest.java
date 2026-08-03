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

import com.autosalone.dtos.OwnerListResponse;
import com.autosalone.dtos.OwnerRequest;
import com.autosalone.dtos.OwnerResponse;
import com.autosalone.enums.TokenType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.AuthToken;
import com.autosalone.models.Owner;
import com.autosalone.repositories.AuthTokenRepository;
import com.autosalone.repositories.OwnerRepository;
import com.autosalone.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

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
        Owner response = ownerService.getOwnerById(ownerId);
        assertNotNull(response);
        assertEquals(mockOwner, response);
    }

    @Test
    void getOwnerById_NotFound() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            ownerService.getOwnerById(ownerId);
        });
    }

    @Test
    void getOwnerResponseById_Success() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(mockOwner));
        when(authTokenRepository.findByUserAndType(mockOwner, TokenType.REGISTRATION)).thenReturn(Optional.empty());
        when(mockOwner.getId()).thenReturn(ownerId);

        OwnerResponse response = ownerService.getOwnerResponseById(ownerId);

        assertNotNull(response);
        assertEquals(ownerId, response.id());
        assertFalse(response.hasActiveInvitation());
    }

    @Test
    void getOwnerResponseById_WithActiveInvitation_Success() {
        AuthToken activeToken = mock(AuthToken.class);
        when(activeToken.isExpired()).thenReturn(false);
        when(mockOwner.getId()).thenReturn(ownerId);

        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(mockOwner));
        when(authTokenRepository.findByUserAndType(mockOwner, TokenType.REGISTRATION))
                .thenReturn(Optional.of(activeToken));

        OwnerResponse response = ownerService.getOwnerResponseById(ownerId);

        assertNotNull(response);
        assertEquals(ownerId, response.id());
        assertTrue(response.hasActiveInvitation());
    }

    @Test
    void getOwnerResponseById_NotFound() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            ownerService.getOwnerResponseById(ownerId);
        });
    }

    @Test
    void getOwners_Success() {
        when(ownerRepository.findOwners(true)).thenReturn(List.of(mockOwner));
        when(mockOwner.getId()).thenReturn(ownerId);

        List<OwnerListResponse> responses = ownerService.getOwners(true);

        assertEquals(1, responses.size());
        assertEquals(ownerId, responses.get(0).id());
        verify(ownerRepository).findOwners(true);
    }

    // write

    @Test
    void addOwner_Success() {
        when(userRepository.findByEmail(ownerRequest.email())).thenReturn(Optional.empty());

        OwnerResponse response = ownerService.addOwner(ownerRequest);

        ArgumentCaptor<Owner> captor = ArgumentCaptor.forClass(Owner.class);
        verify(ownerRepository).save(captor.capture());

        Owner savedOwner = captor.getValue();
        assertEquals("Mario", savedOwner.getFirstName());
        assertEquals("Rossi", savedOwner.getLastName());
        assertEquals("mario.rossi@example.com", savedOwner.getEmail());

        assertNotNull(response);
        assertFalse(response.hasActiveInvitation());
    }

    @Test
    void addOwner_FailsIfEmailAlreadyInUse() {
        when(userRepository.findByEmail(ownerRequest.email())).thenReturn(Optional.of(mockOwner));

        assertThrows(IllegalStateException.class, () -> {
            ownerService.addOwner(ownerRequest);
        });

        verify(ownerRepository, never()).save(any());
    }

    @Test
    void updateOwner_Success_SameEmail() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(mockOwner));
        when(authTokenRepository.findByUserAndType(mockOwner, TokenType.REGISTRATION)).thenReturn(Optional.empty());
        when(mockOwner.getEmail()).thenReturn(ownerRequest.email());

        assertDoesNotThrow(() -> ownerService.updateOwner(ownerId, ownerRequest));

        verify(userRepository, never()).findByEmail(anyString());

        verify(mockOwner).setFirstName("Mario");
        verify(mockOwner).setLastName("Rossi");
        verify(ownerRepository).save(mockOwner);
    }

    @Test
    void updateOwner_Success_DifferentEmail() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(mockOwner));
        when(authTokenRepository.findByUserAndType(mockOwner, TokenType.REGISTRATION)).thenReturn(Optional.empty());
        when(mockOwner.getEmail()).thenReturn("vecchia.email@example.com");

        when(userRepository.findByEmail(ownerRequest.email())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> ownerService.updateOwner(ownerId, ownerRequest));

        verify(userRepository).findByEmail(ownerRequest.email());

        verify(mockOwner).setEmail(ownerRequest.email());
        verify(ownerRepository).save(mockOwner);
    }

    @Test
    void updateOwner_FailsIfDifferentEmailAlreadyInUse() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(mockOwner));
        when(authTokenRepository.findByUserAndType(mockOwner, TokenType.REGISTRATION)).thenReturn(Optional.empty());
        when(mockOwner.getEmail()).thenReturn("vecchia.email@example.com");

        Owner anotherOwner = mock(Owner.class);
        when(userRepository.findByEmail(ownerRequest.email())).thenReturn(Optional.of(anotherOwner));

        assertThrows(IllegalStateException.class, () -> {
            ownerService.updateOwner(ownerId, ownerRequest);
        });

        verify(ownerRepository, never()).save(any());
    }

    @Test
    void updateOwner_FailsIfNullEmailToActiveOwner() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(mockOwner));
        when(authTokenRepository.findByUserAndType(mockOwner, TokenType.REGISTRATION)).thenReturn(Optional.empty());
        when(mockOwner.isActive()).thenReturn(true);
        ownerRequest = new OwnerRequest(
                "Mario",
                "Rossi",
                "3331234567",
                null);

        assertThrows(IllegalStateException.class, () -> {
            ownerService.updateOwner(ownerId, ownerRequest);
        });

        verify(ownerRepository, never()).save(any());
    }
}