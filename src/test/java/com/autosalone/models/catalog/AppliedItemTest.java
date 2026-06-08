package com.autosalone.models.catalog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class AppliedItemTest {

    // base constructor
    @Test
    public void constructor_FromArchivedItem_ThrowsException() {
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));
        accessory.archive();

        assertThrows(IllegalArgumentException.class, () -> new AppliedItem(accessory));
    }

    @Test
    public void constructor_FromValidAccessory_Success() {
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));

        AppliedItem appliedItem = assertDoesNotThrow(() -> new AppliedItem(accessory));
        assertTrue(accessory.getPrice().equals(appliedItem.getAppliedPrice()));
        assertTrue(accessory == appliedItem.getItem());
    }

    @Test
    public void constructor_FromValidAccessoryPackage_Success() {
        AccessoryPackage pack = new AccessoryPackage("Pacchetto", null);
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));
        pack.addItem(accessory);

        AppliedItem appliedItem = assertDoesNotThrow(() -> new AppliedItem(pack));
        assertTrue(pack.getPrice().equals(appliedItem.getAppliedPrice()));
    }

    // copy constructors
    @Test
    public void copyConstructor_FromArchivedItem_Success() {
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));
        AppliedItem accessoryItem = new AppliedItem(accessory);

        accessory.archive();

        assertDoesNotThrow(() -> new AppliedItem(accessoryItem),
                "Should be able to create an applied item from an archived accessory");
        assertTrue(accessoryItem.getItem().isArchived(), "The accessory of the applied item should be archived");
    }

    @Test
    public void copyConstructor_WithGetOriginalPrice_FromArchivedItem_Success() {
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));
        AppliedItem accessoryItem = new AppliedItem(accessory);

        accessory.archive();

        assertDoesNotThrow(() -> new AppliedItem(accessoryItem, true),
                "Should be able to create an applied item from an archived accessory");
        assertTrue(accessoryItem.getItem().isArchived(), "The accessory of the applied item should be archived");
    }

    @Test
    public void copyConstructor_Success() {
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));
        AppliedItem accessoryItem = new AppliedItem(accessory);

        accessory.setBasePrice(new BigDecimal("40.00"));

        AppliedItem clonedAccessoryItem = new AppliedItem(accessoryItem);

        assertEquals(new BigDecimal("40.00"), accessory.getPrice());
        assertEquals(new BigDecimal("50.00"), clonedAccessoryItem.getAppliedPrice(),
                "The cloned accessory should keep the applied price of the item");
    }

    @Test
    public void copyConstructor_WithGetOriginalPrice_Success() {
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));
        AppliedItem accessoryItem = new AppliedItem(accessory);

        accessory.setBasePrice(new BigDecimal("40.00"));

        AppliedItem clonedAccessoryItem = new AppliedItem(accessoryItem, true);

        assertEquals(new BigDecimal("40.00"), accessory.getPrice());
        assertEquals(new BigDecimal("40.00"), clonedAccessoryItem.getAppliedPrice(),
                "The cloned accessory should get the current item base price");
    }

    // set applied price
    @Test
    public void setAppliedPrice_ToNull_ThrowsException() {
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));
        AppliedItem appliedItem = new AppliedItem(accessory);

        assertThrows(NullPointerException.class, () -> appliedItem.setAppliedPrice(null));
    }

    @Test
    public void setAppliedPrice_ToNegative_ThrowsException() {
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));
        AppliedItem appliedItem = new AppliedItem(accessory);

        assertThrows(IllegalArgumentException.class, () -> appliedItem.setAppliedPrice(new BigDecimal("-50.00")));
    }

    @Test
    public void setAppliedPrice_ToValidPrice_Success() {
        Accessory accessory = new Accessory("Nome", null, new BigDecimal("50.00"));
        AppliedItem appliedItem = new AppliedItem(accessory);

        appliedItem.setAppliedPrice(new BigDecimal("40.00"));
        assertTrue(new BigDecimal("40.00").equals(appliedItem.getAppliedPrice()));
        assertTrue(new BigDecimal("50.00").equals(accessory.getPrice()));
    }

}
