package com.autosalone.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public UUID addAccessory(String name, String description, BigDecimal basePrice) {
        Accessory accessory = new Accessory(name, description, basePrice);

        catalogRepository.save(accessory);

        return accessory.getId();
    }

    @Transactional
    public void updateAccessory(UUID accessoryId, String name, String description, BigDecimal basePrice) {
        PurchasableItem item = getItemById(accessoryId);

        if (!(item instanceof Accessory accessory)) {
            throw new IllegalArgumentException("This id does not belong to an accessory: " + accessoryId);
        }

        if (name != null)
            accessory.setName(name);
        if (description != null)
            accessory.setDescription(description);
        if (basePrice != null)
            accessory.setBasePrice(basePrice);

        catalogRepository.save(accessory);
    }

    // accessory package

    @Transactional
    public UUID addAccessoryPackage(String name, String description, Set<UUID> purchasableItemIds) {
        AccessoryPackage accessoryPackage = new AccessoryPackage(name, description);

        if (purchasableItemIds != null && !purchasableItemIds.isEmpty()) {
            for (UUID itemId : purchasableItemIds) {
                PurchasableItem childItem = getItemById(itemId);
                accessoryPackage.addItem(childItem);
            }
        }

        catalogRepository.save(accessoryPackage);
        return accessoryPackage.getId();
    }

    @Transactional
    public void updateAccessoryPackage(UUID accessoryPackageId, String name, String description, Set<UUID> newItemIds) {
        PurchasableItem item = getItemById(accessoryPackageId);

        if (!(item instanceof AccessoryPackage accessoryPackage)) {
            throw new IllegalArgumentException(
                    "This id does not belong to an accessory package: " + accessoryPackageId);
        }

        if (name != null)
            accessoryPackage.setName(name);
        if (description != null)
            accessoryPackage.setDescription(description);

        if (newItemIds != null && !newItemIds.isEmpty()) {
            Set<UUID> currentItemIds = accessoryPackage.getItems().stream()
                    .map(PurchasableItem::getId)
                    .collect(Collectors.toSet());

            // rimuovere gli elementi che non sono più nella nuova lista
            List<PurchasableItem> itemsToRemove = accessoryPackage.getItems().stream()
                    .filter(child -> !newItemIds.contains(child.getId()))
                    .toList();
            for (PurchasableItem child : itemsToRemove) {
                accessoryPackage.removeItem(child);
            }

            // aggiungere i nuovi elementi che prima non c'erano
            for (UUID newId : newItemIds) {
                if (!currentItemIds.contains(newId)) {
                    PurchasableItem childToAdd = getItemById(newId);
                    accessoryPackage.addItem(childToAdd);
                }
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