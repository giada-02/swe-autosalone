package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.OwnerRequest;
import com.autosalone.models.Owner;
import com.autosalone.services.OwnerService;

import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class OwnerControllerTest {

    @Mock
    private OwnerService ownerService;

    @InjectMocks
    private OwnerController ownerController;

    private UUID ownerId;
    private OwnerRequest ownerRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        ownerRequest = new OwnerRequest("Mario", "Rossi", "12345", "test@test.com");
    }

    @Test
    void getOwners_Returns200AndList() {
        Owner mockOwner = mock(Owner.class);
        when(ownerService.getOwners(true)).thenReturn(List.of(mockOwner));

        Response response = ownerController.getOwners(true);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(ownerService).getOwners(true);
    }

    @Test
    void getOwnerById_Returns200AndOwner() {
        Owner mockOwner = mock(Owner.class);
        when(ownerService.getOwnerById(ownerId)).thenReturn(mockOwner);

        Response response = ownerController.getOwnerById(ownerId);

        assertEquals(200, response.getStatus());
        assertEquals(mockOwner, response.getEntity());
    }

    @Test
    void addOwner_Returns201AndLocationHeader() {
        when(ownerService.addOwner(ownerRequest)).thenReturn(ownerId);

        Response response = ownerController.addOwner(ownerRequest);

        assertEquals(201, response.getStatus());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/owners/" + ownerId));
    }

    @Test
    void updateOwner_Returns204NoContent() {
        Response response = ownerController.updateOwner(ownerId, ownerRequest);

        assertEquals(204, response.getStatus());
        assertNull(response.getEntity()); // no body
        verify(ownerService).updateOwner(ownerId, ownerRequest);
    }
}