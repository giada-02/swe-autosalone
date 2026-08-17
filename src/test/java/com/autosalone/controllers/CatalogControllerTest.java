package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.requests.AccessoryPackageRequest;
import com.autosalone.dtos.requests.AccessoryRequest;
import com.autosalone.dtos.responses.CatalogItemResponse;
import com.autosalone.enums.CatalogItemType;
import com.autosalone.services.CatalogService;

import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private CatalogController catalogController;

    private UUID accessoryId;
    private UUID accessoryPackageId;
    private AccessoryRequest accessoryRequest;
    private AccessoryPackageRequest accessoryPackageRequest;
    private CatalogItemResponse accessoryResponse;
    private CatalogItemResponse accessoryPackageResponse;

    @BeforeEach
    void setUp() {
        accessoryId = UUID.randomUUID();
        accessoryPackageId = UUID.randomUUID();
        accessoryRequest = new AccessoryRequest("Accessorio", null, new BigDecimal("50.00"));
        accessoryPackageRequest = new AccessoryPackageRequest("Pacchetto", "Descrizione", null);
        accessoryResponse = new CatalogItemResponse(accessoryId, CatalogItemType.ACCESSORY, "Accessorio", null,
                new BigDecimal("50.00"), false, null);
        accessoryPackageResponse = new CatalogItemResponse(accessoryPackageId, CatalogItemType.PACKAGE, "Pacchetto",
                "Descrizione", null, false, null);
    }

    @Test
    void getCatalogItems_Returns200AndList() {
        when(catalogService.getPurchasableItems(null, false, CatalogItemType.ACCESSORY))
                .thenReturn(List.of(accessoryResponse));

        Response response = catalogController.getCatalogItems(null, false, CatalogItemType.ACCESSORY);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(catalogService).getPurchasableItems(null, false, CatalogItemType.ACCESSORY);
    }

    @Test
    void getCatalogItemById_Returns200AndItem() {
        when(catalogService.getItemResponseById(accessoryId)).thenReturn(accessoryResponse);

        Response response = catalogController.getCatalogItemById(accessoryId);

        assertEquals(200, response.getStatus());
        assertEquals(accessoryResponse, response.getEntity());
        verify(catalogService).getItemResponseById(accessoryId);
    }

    // accessory

    @Test
    void addAccessory_Returns201AndLocationHeaderWithBody() {
        when(catalogService.addAccessory(accessoryRequest)).thenReturn(accessoryResponse);

        Response response = catalogController.addAccessory(accessoryRequest);

        assertEquals(201, response.getStatus());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/catalog/" + accessoryId));

        assertEquals(accessoryResponse, response.getEntity());
    }

    @Test
    void updateAccessory_Returns200AndUpdatedAccessory() {
        when(catalogService.updateAccessory(accessoryId, accessoryRequest)).thenReturn(accessoryResponse);

        Response response = catalogController.updateAccessory(accessoryId, accessoryRequest);

        assertEquals(200, response.getStatus());
        assertEquals(accessoryResponse, response.getEntity());
        verify(catalogService).updateAccessory(accessoryId, accessoryRequest);
    }

    // accessory package

    @Test
    void addAccessoryPackage_Returns201AndLocationHeaderWithBody() {
        when(catalogService.addAccessoryPackage(accessoryPackageRequest))
                .thenReturn(accessoryPackageResponse);

        Response response = catalogController.addAccessoryPackage(accessoryPackageRequest);

        assertEquals(201, response.getStatus());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/catalog/" + accessoryPackageId));

        assertEquals(accessoryPackageResponse, response.getEntity());
    }

    @Test
    void updateAccessoryPackage_Returns200AndUpdatedAccessoryPackage() {
        when(catalogService.updateAccessoryPackage(accessoryPackageId, accessoryPackageRequest))
                .thenReturn(accessoryPackageResponse);

        Response response = catalogController.updateAccessoryPackage(accessoryPackageId,
                accessoryPackageRequest);

        assertEquals(200, response.getStatus());
        assertEquals(accessoryPackageResponse, response.getEntity());
        verify(catalogService).updateAccessoryPackage(accessoryPackageId, accessoryPackageRequest);
    }

    @Test
    void removeCatalogItem_Returns204NoContent() {
        Response response = catalogController.removeCatalogItem(accessoryId);

        assertEquals(204, response.getStatus());
        verify(catalogService).removePurchasableItem(accessoryId);
    }
}
