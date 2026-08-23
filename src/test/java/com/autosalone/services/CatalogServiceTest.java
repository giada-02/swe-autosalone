package com.autosalone.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.requests.AccessoryPackageRequest;
import com.autosalone.dtos.requests.AccessoryRequest;
import com.autosalone.dtos.responses.CatalogItemResponse;
import com.autosalone.enums.CatalogItemType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.catalog.AccessoryPackage;
import com.autosalone.models.catalog.PurchasableItem;
import com.autosalone.repositories.CatalogRepository;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CatalogRepository catalogRepository;

    @InjectMocks
    private CatalogService catalogService;

    private UUID accessoryId;
    private UUID packageId;

    private Accessory accessory;
    private AccessoryPackage accessoryPackage;

    @BeforeEach
    void setUp() {
        accessoryId = UUID.randomUUID();
        packageId = UUID.randomUUID();

        accessory = spy(new Accessory("Navigatore", "GPS Integrato", BigDecimal.valueOf(500)));
        accessoryPackage = spy(new AccessoryPackage("Pacchetto Sport", "Estetica sportiva"));
    }

    // read

    @Test
    void getItemById_Success() {
        when(catalogRepository.findById(accessoryId)).thenReturn(Optional.of(accessory));
        PurchasableItem result = catalogService.getItemById(accessoryId);
        assertNotNull(result);
        assertEquals(accessory, result);
    }

    @Test
    void getItemById_NotFound() {
        when(catalogRepository.findById(accessoryId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            catalogService.getItemById(accessoryId);
        });
    }

    @Test
    void getItemResponseById_Success() {
        doReturn(accessoryId).when(accessory).getId();

        when(catalogRepository.findById(accessoryId)).thenReturn(Optional.of(accessory));

        CatalogItemResponse response = catalogService.getItemResponseById(accessoryId);

        assertNotNull(response);
        assertEquals(accessoryId, response.id());
        assertEquals("Navigatore", response.name());
    }

    @Test
    void getItemResponseById_NotFound() {
        when(catalogRepository.findById(accessoryId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            catalogService.getItemResponseById(accessoryId);
        });
    }

    @Test
    void getPurchasableItems_Success() {
        doReturn(accessoryId).when(accessory).getId();
        doReturn(packageId).when(accessoryPackage).getId();

        when(catalogRepository.findPurchasableItems(anyString(), anyBoolean(), any()))
                .thenReturn(List.of(accessory, accessoryPackage));

        List<CatalogItemResponse> response = catalogService.getPurchasableItems("Nav", false,
                CatalogItemType.ACCESSORY);

        assertEquals(2, response.size());
        verify(catalogRepository).findPurchasableItems("Nav", false, Accessory.class);
    }

    // write

    // accessory

    @Test
    void addAccessory_Success() {
        AccessoryRequest request = new AccessoryRequest("Tetto Panoramico", null, BigDecimal.valueOf(1200));

        CatalogItemResponse accessory = catalogService.addAccessory(request);

        assertNotNull(accessory);
        verify(catalogRepository).save(any(Accessory.class));
    }

    @Test
    void updateAccessory_Success() {
        doReturn(accessoryId).when(accessory).getId();

        AccessoryRequest request = new AccessoryRequest("Navigatore Pro", "Schermo 12 pollici",
                BigDecimal.valueOf(800));
        when(catalogRepository.findById(accessoryId)).thenReturn(Optional.of(accessory));

        catalogService.updateAccessory(accessoryId, request);

        assertEquals("Navigatore Pro", accessory.getName());
        assertEquals("Schermo 12 pollici", accessory.getDescription());
        assertEquals(BigDecimal.valueOf(800), accessory.getPrice());
        verify(catalogRepository).save(accessory);
    }

    @Test
    void updateAccessory_FailsIfItemIsPackage() {
        AccessoryRequest request = new AccessoryRequest("Test", null, BigDecimal.TEN);
        when(catalogRepository.findById(packageId)).thenReturn(Optional.of(accessoryPackage));

        assertThrows(ResourceNotFoundException.class, () -> {
            catalogService.updateAccessory(packageId, request);
        });

        verify(catalogRepository, never()).save(any());
    }

    // accessory package

    @Test
    void addAccessoryPackage_Success() {
        doReturn(accessoryId).when(accessory).getId();

        AccessoryPackageRequest request = new AccessoryPackageRequest("Pacchetto Test", null, Set.of(accessoryId));
        when(catalogRepository.findById(accessoryId)).thenReturn(Optional.of(accessory));

        CatalogItemResponse response = catalogService.addAccessoryPackage(request);

        assertNotNull(response);
        verify(catalogRepository).save(any(AccessoryPackage.class));
    }

    @Test
    void addAccessoryPackage_WithItems_Success() {
        doReturn(accessoryId).when(accessory).getId();

        AccessoryPackageRequest request = new AccessoryPackageRequest("Pacchetto Test", null, Set.of(accessoryId));
        when(catalogRepository.findById(accessoryId)).thenReturn(Optional.of(accessory));

        catalogService.addAccessoryPackage(request);

        ArgumentCaptor<AccessoryPackage> packageCaptor = ArgumentCaptor.forClass(AccessoryPackage.class);
        verify(catalogRepository).save(packageCaptor.capture());

        AccessoryPackage savedPackage = packageCaptor.getValue();

        assertEquals(1, savedPackage.getItems().size());
        assertEquals("Navigatore", savedPackage.getItems().get(0).getName());
    }

    @Test
    void updateAccessoryPackage_FailsIfItemIsAccessory() {
        AccessoryPackageRequest request = new AccessoryPackageRequest("Test", null, null);
        when(catalogRepository.findById(accessoryId)).thenReturn(Optional.of(accessory));

        assertThrows(ResourceNotFoundException.class, () -> {
            catalogService.updateAccessoryPackage(accessoryId, request);
        });

        verify(catalogRepository, never()).save(any());
    }

    @Test
    void updateAccessoryPackage_Success() {
        UUID oldChildId = UUID.randomUUID();
        UUID newChildId = UUID.randomUUID();

        Accessory oldChild = spy(new Accessory("Vecchio", "Desc", BigDecimal.TEN));
        doReturn(oldChildId).when(oldChild).getId();

        Accessory newChild = spy(new Accessory("Nuovo", "Desc", BigDecimal.TEN));
        doReturn(newChildId).when(newChild).getId();

        doReturn(packageId).when(accessoryPackage).getId();

        accessoryPackage.getItems().add(oldChild);

        when(catalogRepository.findById(packageId)).thenReturn(Optional.of(accessoryPackage));
        when(catalogRepository.findById(newChildId)).thenReturn(Optional.of(newChild));

        AccessoryPackageRequest request = new AccessoryPackageRequest("Nuovo Pacchetto", null, Set.of(newChildId));

        catalogService.updateAccessoryPackage(packageId, request);

        assertEquals("Nuovo Pacchetto", accessoryPackage.getName());
        assertEquals(1, accessoryPackage.getItems().size());
        assertTrue(accessoryPackage.getItems().contains(newChild));
        assertFalse(accessoryPackage.getItems().contains(oldChild));

        verify(catalogRepository).save(accessoryPackage);
    }

    @Test
    void updateAccessoryPackage_NullOrEmptyList_ClearsThePackage() {
        AccessoryPackageRequest request = new AccessoryPackageRequest("Pacchetto Vuoto", null, Collections.emptySet());

        Accessory child = mock(Accessory.class);
        accessoryPackage.getItems().add(child);

        when(catalogRepository.findById(packageId)).thenReturn(Optional.of(accessoryPackage));

        catalogService.updateAccessoryPackage(packageId, request);

        assertEquals("Pacchetto Vuoto", accessoryPackage.getName());
        assertTrue(accessoryPackage.getItems().isEmpty());
        verify(catalogRepository).save(accessoryPackage);
    }

    // delete

    @Test
    void removePurchasableItem_HardDelete_WhenNotInUse() {
        when(catalogRepository.findById(accessoryId)).thenReturn(Optional.of(accessory));
        when(catalogRepository.isItemInUse(accessoryId)).thenReturn(false);

        catalogService.removePurchasableItem(accessoryId);

        verify(catalogRepository).delete(accessory);
        verify(catalogRepository, never()).save(any());
        assertFalse(accessory.isArchived());
    }

    @Test
    void removePurchasableItem_SoftDelete_WhenInUse() {
        when(catalogRepository.findById(accessoryId)).thenReturn(Optional.of(accessory));
        when(catalogRepository.isItemInUse(accessoryId)).thenReturn(true);

        catalogService.removePurchasableItem(accessoryId);

        verify(catalogRepository, never()).delete(any());
        verify(catalogRepository).save(accessory);
        assertTrue(accessory.isArchived());
    }
}