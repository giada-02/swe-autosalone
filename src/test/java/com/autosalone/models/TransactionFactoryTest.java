package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.autosalone.enums.VehicleCondition;

public class TransactionFactoryTest {
    @Test
    public void createVehiclePurchase_ForVehicleWithPurchaseTransactionAlready_ThrowsException() {
        Vehicle car = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setIsInShowroom(true)
                .setCondition(VehicleCondition.SECONDHAND)
                .build();
        Transaction purchaseTransaction = TransactionFactory.createVehiclePurchase(car, new BigDecimal("9000"),
                LocalDate.now());
        car.setPurchaseTransaction(purchaseTransaction);

        assertNotNull(car.getPurchaseTransaction());
        assertThrows(IllegalStateException.class,
                () -> TransactionFactory.createVehiclePurchase(car, new BigDecimal("8000"), LocalDate.now()));
    }
}
