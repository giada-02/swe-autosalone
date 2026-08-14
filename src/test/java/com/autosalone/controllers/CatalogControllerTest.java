package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
import com.autosalone.enums.CatalogItemType;
import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.catalog.AccessoryPackage;
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

    @BeforeEach
    void setUp() {
        accessoryId = UUID.randomUUID();
        accessoryPackageId = UUID.randomUUID();
        accessoryRequest = new AccessoryRequest("Accessorio", null, new BigDecimal("50.00"));
        accessoryPackageRequest = new AccessoryPackageRequest("Pacchetto", "Descrizione", null);
    }

    @Test
    void getCatalogItems_Returns200AndList() {
        Accessory mockAccessory = mock(Accessory.class);
        when(catalogService.getPurchasableItems(null, false, CatalogItemType.ACCESSORY))
                .thenReturn(List.of(mockAccessory));

        Response response = catalogController.getCatalogItems(null, false, CatalogItemType.ACCESSORY);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(catalogService).getPurchasableItems(null, false, CatalogItemType.ACCESSORY);
    }

    @Test
    void getCatalogItemById_Returns200AndItem() {
        Accessory mockAccessory = mock(Accessory.class);
        when(catalogService.getItemById(accessoryId)).thenReturn(mockAccessory);
        when(mockAccessory.getId()).thenReturn(accessoryId);
        when(mockAccessory.getName()).thenReturn("Accessorio");

        Response response = catalogController.getCatalogItemById(accessoryId);

        assertEquals(200, response.getStatus());

        Accessory catalogResponse = (Accessory) response.getEntity();
        assertEquals(accessoryId, catalogResponse.getId());
        assertEquals("Accessorio", catalogResponse.getName());

        verify(catalogService).getItemById(accessoryId);
    }

    // accessory

    @Test
    void addAccessory_Returns201AndLocationHeaderWithBody() {
        Accessory mockAccessory = mock(Accessory.class);
        when(mockAccessory.getId()).thenReturn(accessoryId);
        when(catalogService.addAccessory(accessoryRequest)).thenReturn(mockAccessory);

        Response response = catalogController.addAccessory(accessoryRequest);

        assertEquals(201, response.getStatus());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/catalog/" + accessoryId));

        assertEquals(mockAccessory, response.getEntity());
    }

    @Test
    void updateAccessory_Returns200AndUpdatedAccessory() {
        Accessory mockAccessory = mock(Accessory.class);
        when(catalogService.updateAccessory(accessoryId, accessoryRequest)).thenReturn(mockAccessory);

        Response response = catalogController.updateAccessory(accessoryId, accessoryRequest);

        assertEquals(200, response.getStatus());
        assertEquals(mockAccessory, response.getEntity());
        verify(catalogService).updateAccessory(accessoryId, accessoryRequest);
    }

    // accessory package

    @Test
    void addAccessoryPackage_Returns201AndLocationHeaderWithBody() {
        AccessoryPackage mockAccessoryPackage = mock(AccessoryPackage.class);
        when(mockAccessoryPackage.getId()).thenReturn(accessoryPackageId);
        when(catalogService.addAccessoryPackage(accessoryPackageRequest))
                .thenReturn(mockAccessoryPackage);

        Response response = catalogController.addAccessoryPackage(accessoryPackageRequest);

        assertEquals(201, response.getStatus());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/catalog/" + accessoryPackageId));

        assertEquals(mockAccessoryPackage, response.getEntity());
    }

    @Test
    void updateAccessoryPackage_Returns200AndUpdatedAccessoryPackage() {
        AccessoryPackage mockAccessoryPackage = mock(AccessoryPackage.class);
        when(catalogService.updateAccessoryPackage(accessoryPackageId, accessoryPackageRequest))
                .thenReturn(mockAccessoryPackage);

        Response response = catalogController.updateAccessoryPackage(accessoryPackageId,
                accessoryPackageRequest);

        assertEquals(200, response.getStatus());
        assertEquals(mockAccessoryPackage, response.getEntity());
        verify(catalogService).updateAccessoryPackage(accessoryPackageId, accessoryPackageRequest);
    }

    @Test
    void removeCatalogItem_Returns204NoContent() {
        Response response = catalogController.removeCatalogItem(accessoryId);

        assertEquals(204, response.getStatus());
        verify(catalogService).removePurchasableItem(accessoryId);
    }
}
