package com.autosalone.models.catalog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class AccessoryPackageTest {

    @Test
    public void archive_ItemsInsideShouldNotBeArchived() {
        AccessoryPackage pack = new AccessoryPackage("Pacchetto", "Descrizione");
        Accessory accessory = new Accessory("Accessorio", "Descrizione", new BigDecimal("50.00"));
        Accessory innerPack = new Accessory("Pacchetto Interno", null, new BigDecimal("50.00"));
        pack.addItem(accessory);
        pack.addItem(innerPack);

        pack.archive();
        assertTrue(pack.isArchived());
        assertFalse(accessory.isArchived());
        assertFalse(innerPack.isArchived());
    }

    @Test
    public void addItem_Self_ThrowsException() {
        AccessoryPackage pack = new AccessoryPackage("Pacchetto", "Descrizione");
        assertThrows(IllegalArgumentException.class, () -> pack.addItem(pack));
    }

    @Test
    public void addItem_AlreadyExisting_ThrowsException() {
        AccessoryPackage pack = new AccessoryPackage("Pacchetto", "Descrizione");
        Accessory accessory = new Accessory("Accessorio", "Descrizione", new BigDecimal("50.00"));
        pack.addItem(accessory);
        assertThrows(IllegalArgumentException.class, () -> pack.addItem(accessory));
    }

    @Test
    public void addItem_IsArchived_ThrowsException() {
        AccessoryPackage pack = new AccessoryPackage("Pacchetto", "Descrizione");
        Accessory accessory = new Accessory("Accessorio", "Descrizione", new BigDecimal("50.00"));
        accessory.archive();

        assertThrows(IllegalArgumentException.class, () -> pack.addItem(pack));
    }

    @Test
    public void addItem_Success() {
        AccessoryPackage pack = new AccessoryPackage("Pacchetto", "Descrizione");
        Accessory accessory = new Accessory("Accessorio", "Descrizione", new BigDecimal("50.00"));
        pack.addItem(accessory);
        assertTrue(pack.getItems().size() == 1);
        assertTrue(pack.getItems().contains(accessory));
    }

    @Test
    public void addItem_WhenArchived_ThrowsException() {
        AccessoryPackage pack = new AccessoryPackage("Pacchetto", "Descrizione");
        pack.archive();
        Accessory accessory = new Accessory("Accessorio", "Descrizione", new BigDecimal("50.00"));

        assertThrows(IllegalStateException.class, () -> pack.addItem(accessory));
        assertTrue(pack.getItems().size() == 0);
    }

    @Test
    public void removeItem_Success() {
        AccessoryPackage pack = new AccessoryPackage("Pacchetto", "Descrizione");
        AccessoryPackage innerPack = new AccessoryPackage("Pacchetto Interno", null);

        pack.addItem(innerPack);
        pack.removeItem(innerPack);
        assertTrue(pack.getItems().isEmpty());
    }

    @Test
    public void removeItem_WhenArchived_ThrowsException() {
        AccessoryPackage pack = new AccessoryPackage("Pacchetto", "Descrizione");
        Accessory accessory = new Accessory("Accessorio", "Descrizione", new BigDecimal("50.00"));
        pack.addItem(accessory);
        pack.archive();

        assertThrows(IllegalStateException.class, () -> pack.removeItem(accessory));
        assertTrue(pack.getItems().size() == 1);
    }

    @Test
    public void getPrice_WithNestedPackages_CalculatesRecursiveTotalCorrectly() {
        AccessoryPackage comfortPack = new AccessoryPackage("Pacchetto inverno", null);
        comfortPack.addItem(new Accessory("Sedili riscaldabili", "Interno", new BigDecimal("400.00")));
        comfortPack.addItem(new Accessory("Volante riscaldabile", "Interno", new BigDecimal("200.00")));

        AccessoryPackage ultimateWinterPack = new AccessoryPackage("Pacchetto inverno completo", null);
        ultimateWinterPack.addItem(new Accessory("Catene da neve", "Esterno", new BigDecimal("150.00")));
        ultimateWinterPack.addItem(comfortPack);

        BigDecimal grandTotal = ultimateWinterPack.getPrice();

        assertTrue(new BigDecimal("750.00").equals(grandTotal),
                "The composite pattern should recursively sum all nested items and packages");
    }

    @Test
    public void getPrice_EmptyPackage_ReturnsZero() {
        AccessoryPackage emptyPack = new AccessoryPackage("Pacchetto vuoto", null);

        BigDecimal total = emptyPack.getPrice();

        assertTrue(BigDecimal.ZERO.equals(total),
                "An empty package should safely return a total of zero");
    }
}
