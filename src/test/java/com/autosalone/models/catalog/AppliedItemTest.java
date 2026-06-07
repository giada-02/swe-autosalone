package com.autosalone.models.catalog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class AppliedItemTest {
    @Test
    public void constructor_FromArchivedItem_ThrowsException() {
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));
        accessory.archive();

        assertThrows(IllegalStateException.class, () -> new AppliedItem(accessory));
    }

    @Test
    public void constructor_FromValidAccessory_Success() {
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));

        AppliedItem appliedItem = assertDoesNotThrow(() -> new AppliedItem(accessory));
        assertTrue(accessory.getPrice().equals(appliedItem.getAppliedPrice()));
        assertTrue(accessory == appliedItem.getItem());
    }

    @Test
    public void constructor_FromEmptyAccessoryPackage_ThrowsException() {
        AccessoryPackage pack = new AccessoryPackage("Pacchetto", null);

        assertThrows(IllegalStateException.class, () -> new AppliedItem(pack));
    }

    @Test
    public void constructor_FromValidAccessoryPackage_Success() {
        AccessoryPackage pack = new AccessoryPackage("Pacchetto", null);
        Accessory accessory = new Accessory("Name", null, new BigDecimal("50.00"));
        pack.addItem(accessory);

        AppliedItem appliedItem = assertDoesNotThrow(() -> new AppliedItem(pack));
        assertTrue(pack.getPrice().equals(appliedItem.getAppliedPrice()));

    }

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
