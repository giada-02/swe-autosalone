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

import com.autosalone.dtos.requests.OwnerRequest;
import com.autosalone.dtos.responses.OwnerListResponse;
import com.autosalone.dtos.responses.OwnerResponse;
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
        OwnerListResponse ownerListResponse = new OwnerListResponse(ownerId, "Mario", "Rossi", "12345", "test@test.com",
                true);
        when(ownerService.getOwners(true)).thenReturn(List.of(ownerListResponse));

        Response response = ownerController.getOwners(true);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(ownerService).getOwners(true);
    }

    @Test
    void getOwnerById_Returns200AndOwner() {
        OwnerResponse mockOwnerResponse = mock(OwnerResponse.class);
        when(mockOwnerResponse.id()).thenReturn(ownerId);
        when(mockOwnerResponse.phoneNumber()).thenReturn("3331234567");
        when(ownerService.getOwnerResponseById(ownerId)).thenReturn(mockOwnerResponse);

        Response response = ownerController.getOwnerById(ownerId);

        assertEquals(200, response.getStatus());

        OwnerResponse ownerResponse = (OwnerResponse) response.getEntity();
        assertEquals(ownerId, ownerResponse.id());
        assertEquals("3331234567", ownerResponse.phoneNumber());

        verify(ownerService).getOwnerResponseById(ownerId);
    }

    @Test
    void addOwner_Returns201AndLocationHeaderWithBody() {
        OwnerResponse mockOwnerResponse = mock(OwnerResponse.class);
        when(mockOwnerResponse.id()).thenReturn(ownerId);
        when(ownerService.addOwner(ownerRequest)).thenReturn(mockOwnerResponse);

        Response response = ownerController.addOwner(ownerRequest);

        assertEquals(201, response.getStatus());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/owners/" + ownerId));

        assertEquals(mockOwnerResponse, response.getEntity());
    }

    @Test
    void updateOwner_Returns200AndUpdatedOwner() {
        OwnerResponse mockOwnerResponse = mock(OwnerResponse.class);
        when(ownerService.updateOwner(ownerId, ownerRequest)).thenReturn(mockOwnerResponse);

        Response response = ownerController.updateOwner(ownerId, ownerRequest);

        assertEquals(200, response.getStatus());
        assertEquals(mockOwnerResponse, response.getEntity());
        verify(ownerService).updateOwner(ownerId, ownerRequest);
    }
}