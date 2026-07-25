package com.autosalone.services;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.autosalone.dtos.AccessoryRequest;
import com.autosalone.dtos.AccessoryPackageRequest;
import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.catalog.AccessoryPackage;
import com.autosalone.models.catalog.PurchasableItem;
import com.autosalone.repositories.CatalogRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CatalogService {

    @Inject
    private CatalogRepository catalogRepository;

    // read

    public PurchasableItem getItemById(UUID id) {
        return catalogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found of id: " + id));
    }

    public List<PurchasableItem> getPurchasableItems(String keyword, Boolean isArchived,
            Class<? extends PurchasableItem> itemType) {
        return catalogRepository.findPurchasableItems(keyword, isArchived, itemType);
    }

    // write

    // accessory

    @Transactional
    public UUID addAccessory(AccessoryRequest request) {
        Accessory accessory = new Accessory(request.name(), request.description(), request.basePrice());
        catalogRepository.save(accessory);
        return accessory.getId();
    }

    @Transactional
    public void updateAccessory(UUID accessoryId, AccessoryRequest request) {
        PurchasableItem item = getItemById(accessoryId);

        if (!(item instanceof Accessory accessory)) {
            throw new IllegalArgumentException("This id does not belong to an accessory: " + accessoryId);
        }

        accessory.setName(request.name());
        accessory.setDescription(request.description());
        accessory.setBasePrice(request.basePrice());

        catalogRepository.save(accessory);
    }

    // accessory package

    @Transactional
    public UUID addAccessoryPackage(AccessoryPackageRequest request) {
        AccessoryPackage accessoryPackage = new AccessoryPackage(request.name(), request.description());

        if (request.purchasableItemIds() == null || request.purchasableItemIds().isEmpty()) {
            catalogRepository.save(accessoryPackage);
            return accessoryPackage.getId();
        }

        for (UUID itemId : request.purchasableItemIds()) {
            PurchasableItem childItem = getItemById(itemId);
            accessoryPackage.addItem(childItem);
        }

        catalogRepository.save(accessoryPackage);
        return accessoryPackage.getId();
    }

    @Transactional
    public void updateAccessoryPackage(UUID accessoryPackageId, AccessoryPackageRequest request) {
        PurchasableItem item = getItemById(accessoryPackageId);

        if (!(item instanceof AccessoryPackage accessoryPackage)) {
            throw new IllegalArgumentException(
                    "This id does not belong to an accessory package: " + accessoryPackageId);
        }

        accessoryPackage.setName(request.name());
        accessoryPackage.setDescription(request.description());

        Set<UUID> safeNewItemIds = (request.purchasableItemIds() == null) ? Collections.emptySet()
                : request.purchasableItemIds();

        Set<UUID> currentItemIds = accessoryPackage.getItems().stream()
                .map(PurchasableItem::getId)
                .collect(Collectors.toSet());

        if (safeNewItemIds.equals(currentItemIds)) {
            catalogRepository.save(accessoryPackage);
            return;
        }

        // rimuovere gli elementi che non sono più nella nuova lista
        // se safeNewItemIds è vuoto rimuoverà tutto
        List<PurchasableItem> itemsToRemove = accessoryPackage.getItems().stream()
                .filter(child -> !safeNewItemIds.contains(child.getId()))
                .toList();

        for (PurchasableItem child : itemsToRemove) {
            accessoryPackage.removeItem(child);
        }

        // aggiungere i nuovi elementi che prima non c'erano
        for (UUID newId : safeNewItemIds) {
            if (!currentItemIds.contains(newId)) {
                PurchasableItem childToAdd = getItemById(newId);
                accessoryPackage.addItem(childToAdd);
            }
        }

        catalogRepository.save(accessoryPackage);
    }

    // delete

    @Transactional
    public void removePurchasableItem(UUID itemId) {
        PurchasableItem item = getItemById(itemId);

        if (catalogRepository.isItemInUse(itemId)) {
            item.archive();
            catalogRepository.save(item);
        } else {
            catalogRepository.delete(item);
        }
    }
}