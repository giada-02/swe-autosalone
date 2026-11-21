package com.autosalone.models.catalog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AccessoryTest {

    private Accessory accessory;

    @BeforeEach
    public void setUp() {
        accessory = new Accessory("Nome", "Descrizione", new BigDecimal("50.00"));
    }

    @Test
    public void constructor_WithNullName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Accessory(null, "Descrizione", new BigDecimal("50.00")));
    }

    @Test
    public void constructor_WithNullBasePrice_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Accessory("Nome", "Descrizione", null));
    }

    @Test
    public void constructor_WithNegativeBasePrice_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Accessory("Nome", "Descrizione", new BigDecimal("-50.00")));
    }

    @Test
    public void setName_ToNull_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> accessory.setName(null));
    }

    @Test
    public void setDescription_ToNull_Success() {
        assertDoesNotThrow(() -> accessory.setDescription(null));
    }

    @Test
    public void setBasePrice_ToNull_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> accessory.setBasePrice(null));
    }

    @Test
    public void setBasePrice_ToNegative_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> accessory.setBasePrice(new BigDecimal("-50.00")));
    }

    @Test
    public void archive_Success() {
        assertDoesNotThrow(() -> accessory.archive());
        assertTrue(accessory.isArchived());
    }

    @Test
    public void setName_WhenArchived_ThrowsException() {
        accessory.archive();
        assertThrows(IllegalStateException.class, () -> accessory.setName("Nuovo nome"));
    }

    @Test
    public void setDescription_WhenArchived_ThrowsException() {
        accessory.archive();
        assertThrows(IllegalStateException.class, () -> accessory.setDescription("Nuova Descrizione"));
    }

    @Test
    public void setBasePrice_WhenArchived_ThrowsException() {
        accessory.archive();
        assertThrows(IllegalStateException.class, () -> accessory.setBasePrice(new BigDecimal("40.00")));
    }

    @Test
    public void setBasePrice_ToValidPrice_Success() {
        BigDecimal newPrice = new BigDecimal("40.00");
        accessory.setBasePrice(newPrice);
        assertEquals(0, accessory.getPrice().compareTo(newPrice));
    }

    @Test
    public void hasSameDetails_SameNameSameDescription_ReturnsTrue() {
        assertTrue(accessory.hasSameDetails("  Nome ", " Descrizione  "));
    }

    @Test
    public void hasSameDetails_SameNameDifferentDescription_ReturnsFalse() {
        assertFalse(accessory.hasSameDetails("Nome", null));
    }

    @Test
    public void hasSameDetails_SameDescriptionDifferentName_ReturnsFalse() {
        assertFalse(accessory.hasSameDetails("Nome diverso", "Descrizione"));
    }

    @Test
    public void hasSameDetails_SameNameNullDescription_ReturnsTrue() {
        accessory.setDescription(null);
        assertTrue(accessory.hasSameDetails("Nome", ""));
        assertTrue(accessory.hasSameDetails("Nome", null));
    }
}
