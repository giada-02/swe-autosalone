package com.autosalone.models.catalog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class AccessoryTest {
    @Test
    public void constructor_WithNullName_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new Accessory(null, "Descrizione", new BigDecimal("50.00")));
    }

    @Test
    public void constructor_WithNullBasePrice_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new Accessory("Nome", "Descrizione", null));
    }

    @Test
    public void constructor_WithNegativeBasePrice_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Accessory("Nome", "Descrizione", new BigDecimal("-50.00")));
    }

    @Test
    public void setName_ToNull_ThrowsException() {
        Accessory accessory = new Accessory("Nome", null, new BigDecimal("50.00"));

        assertThrows(NullPointerException.class, () -> accessory.setName(null));
    }

    @Test
    public void setDescription_ToNull_Success() {
        Accessory accessory = new Accessory("Nome", "Descrizione", new BigDecimal("50.00"));

        assertDoesNotThrow(() -> accessory.setDescription(null));
    }

    @Test
    public void setBasePrice_ToNull_ThrowsException() {
        Accessory accessory = new Accessory("Nome", "Descrizione", new BigDecimal("50.00"));

        assertThrows(NullPointerException.class, () -> accessory.setBasePrice(null));
    }

    @Test
    public void setBasePrice_ToNegative_ThrowsException() {
        Accessory accessory = new Accessory("Nome", "Descrizione", new BigDecimal("50.00"));

        assertThrows(IllegalArgumentException.class,
                () -> accessory.setBasePrice(new BigDecimal("-50.00")));
    }

    @Test
    public void archive_Success() {
        Accessory accessory = new Accessory("Nome", null, new BigDecimal("50.00"));
        assertDoesNotThrow(() -> accessory.archive());
        assertTrue(accessory.isArchived());
    }

    @Test
    public void setName_WhenArchived_ThrowsException() {
        Accessory accessory = new Accessory("Nome", null, new BigDecimal("50.00"));
        accessory.archive();

        assertThrows(IllegalStateException.class, () -> accessory.setName("Nuovo nome"));
    }

    @Test
    public void setDescription_WhenArchived_ThrowsException() {
        Accessory accessory = new Accessory("Nome", null, new BigDecimal("50.00"));
        accessory.archive();

        assertThrows(IllegalStateException.class, () -> accessory.setDescription("Descrizione"));
    }

    @Test
    public void setBasePrice_WhenArchived_ThrowsException() {
        Accessory accessory = new Accessory("Nome", null, new BigDecimal("50.00"));
        accessory.archive();

        assertThrows(IllegalStateException.class, () -> accessory.setBasePrice(new BigDecimal("40.00")));
    }

    @Test
    public void setBasePrice_ToValidPrice_Success() {
        Accessory accessory = new Accessory("Nome", null, new BigDecimal("50.00"));
        BigDecimal newPrice = new BigDecimal("40.00");

        accessory.setBasePrice(newPrice);

        assertTrue(newPrice.equals(accessory.getPrice()));
    }
}
