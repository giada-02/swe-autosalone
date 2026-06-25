package com.autosalone.repositories;

import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.catalog.AccessoryPackage;
import com.autosalone.models.catalog.PurchasableItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CatalogRepositoryTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private CatalogRepository repository;

    @BeforeEach
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("autosalonePU-test");
        em = emf.createEntityManager();
        repository = new CatalogRepository();
        repository.em = this.em;
    }

    @AfterEach
    public void tearDown() {
        if (em != null)
            em.close();
        if (emf != null)
            emf.close();
    }

    @Test
    public void savePurchasableItem_Success() {
        em.getTransaction().begin();

        Accessory accessory = new Accessory("Articolo", null, new BigDecimal("50.00"));
        AccessoryPackage accessoryPackage = new AccessoryPackage("Pacchetto", "Generico");
        accessoryPackage.addItem(accessory);

        repository.save(accessory);
        repository.save(accessoryPackage);
        em.getTransaction().commit();
        assertNotNull(accessory.getId());
        assertNotNull(accessoryPackage.getId());

        em.clear();

        Optional<PurchasableItem> foundAccessory = repository.findById(accessory.getId());
        assertTrue(foundAccessory.isPresent());
        assertEquals("Articolo", foundAccessory.get().getName());
        assertNull(foundAccessory.get().getDescription());
        assertTrue(new BigDecimal("50.00").equals(foundAccessory.get().getPrice()));
        Optional<PurchasableItem> foundAccessoryPackage = repository.findById(accessoryPackage.getId());
        assertTrue(foundAccessoryPackage.isPresent());
        assertEquals("Pacchetto", foundAccessoryPackage.get().getName());
        assertEquals("Generico", foundAccessoryPackage.get().getDescription());
        assertTrue(new BigDecimal("50.00").equals(foundAccessoryPackage.get().getPrice()));
    }

    @Test
    public void findById_NotFound() {
        Optional<PurchasableItem> found = repository.findById(UUID.randomUUID());
        assertTrue(found.isEmpty(), "Should be empty, the ID does not exist");
    }

    @Test
    public void findPurchasableItems_WithDynamicFilters_ReturnsCorrectResults() {
        em.getTransaction().begin();

        Accessory accessory1 = new Accessory("Articolo1", null, new BigDecimal("50.00"));
        repository.save(accessory1);

        Accessory accessory2 = new Accessory("Articolo2", null, new BigDecimal("30.00"));
        accessory2.archive(); // archived
        repository.save(accessory2);

        AccessoryPackage accessoryPackage = new AccessoryPackage("Pacchetto", "Generico");
        accessoryPackage.addItem(accessory1);
        repository.save(accessoryPackage);

        em.getTransaction().commit();
        em.clear();

        List<PurchasableItem> byKeyword = repository.findPurchasableItems("articolo", null, null); // case-insensitive
        assertEquals(2, byKeyword.size(), "Should find 2 purchasable items");
        assertEquals("Articolo1", byKeyword.get(0).getName());
        assertEquals("Articolo2", byKeyword.get(1).getName());

        List<PurchasableItem> archived = repository.findPurchasableItems(null, true, null);
        assertEquals(1, archived.size(), "Should find 1 ARCHIVED purchasable item");
        assertTrue(archived.get(0).isArchived());

        List<PurchasableItem> byItemType = repository.findPurchasableItems(null, null, AccessoryPackage.class);
        assertEquals(1, byItemType.size(), "Should find 1 accessory package");
    }

    @Test
    public void savePurchasableItem_UpdateMerge_Success() {
        em.getTransaction().begin();

        Accessory accessory = new Accessory("Articolo", null, new BigDecimal("50.00"));

        repository.save(accessory);
        em.getTransaction().commit();

        UUID accessoryId = accessory.getId();

        em.clear();
        em.getTransaction().begin();

        PurchasableItem accessoryToUpdate = repository.findById(accessoryId).get();
        accessoryToUpdate.setDescription("1");
        accessoryToUpdate.archive();

        repository.save(accessoryToUpdate); // em.merge()
        em.getTransaction().commit();
        em.clear();

        PurchasableItem updatedAccessory = repository.findById(accessoryId).get();
        assertEquals("1", updatedAccessory.getDescription(), "Description should be updated");
        assertTrue(updatedAccessory.isArchived());
    }

    @Test
    public void deletePurchasableItem_AttachedEntity_Success() {
        em.getTransaction().begin();

        Accessory purchasableItem = new Accessory("Articolo", null, new BigDecimal("50.00"));

        repository.save(purchasableItem);

        repository.delete(purchasableItem); // em.remove(purchasableItem)
        em.getTransaction().commit();
        em.clear();

        Optional<PurchasableItem> deletedPurchasableItem = repository.findById(purchasableItem.getId());
        assertTrue(deletedPurchasableItem.isEmpty(), "Should be empty, the purchasableItem has been eliminated");
    }

    @Test
    public void deletePurchasableItem_DetachedEntity_Success() {
        em.getTransaction().begin();
        AccessoryPackage purchasableItem = new AccessoryPackage("Pacchetto", "Generico");

        repository.save(purchasableItem);
        em.getTransaction().commit();

        UUID purchasableItemId = purchasableItem.getId();
        assertNotNull(purchasableItemId);

        em.clear();
        em.getTransaction().begin();

        PurchasableItem purchasableItemToDelete = repository.findById(purchasableItemId)
                .orElseThrow(() -> new IllegalStateException("PurchasableItem not found"));

        em.clear();
        repository.delete(purchasableItemToDelete); // em.remove(em.merge(purchasableItem))

        em.getTransaction().commit();
        em.clear();

        Optional<PurchasableItem> deletedPurchasableItem = repository.findById(purchasableItemId);
        assertTrue(deletedPurchasableItem.isEmpty(), "Should be empty, the purchasableItem has been eliminated");
    }

}