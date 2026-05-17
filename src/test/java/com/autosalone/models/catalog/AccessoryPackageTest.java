package com.autosalone.models.catalog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class AccessoryPackageTest {
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
