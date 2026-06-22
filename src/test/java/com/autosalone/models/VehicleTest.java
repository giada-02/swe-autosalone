package com.autosalone.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;

public class VehicleTest {

    private Vehicle.VehicleBuilder baseBuilder;

    @BeforeEach
    public void setUp() {
        baseBuilder = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setIsInShowroom(true);
    }

    @Test
    public void vehicleBuilder_CreatesVehicleInAvailableStatus() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.NEW).build();

        assertEquals(VehicleStatus.AVAILABLE, car.getStatus());
        assertTrue(car.isInShowroom());
    }

    @Test
    public void generateStandardInspectionDeadline_SetsInspectionTo4YearsEndOfMonth() {
        LocalDate today = LocalDate.now();
        Vehicle car = baseBuilder.setCondition(VehicleCondition.SECONDHAND)
                .setRegistrationDate(today)
                .build();

        car.generateStandardInspectionDeadline();

        assertEquals(1, car.getDeadlines().size());
        Deadline inspectionDeadline = car.getDeadlines().get(0);

        LocalDate expectedDate = today.plusYears(4)
                .with(TemporalAdjusters.lastDayOfMonth());

        assertEquals("Revisione Veicolo", inspectionDeadline.getReason());
        assertTrue(expectedDate.isEqual(inspectionDeadline.getDueDate()));
        assertEquals(2, inspectionDeadline.getRecurrence().getYears());
        assertTrue(inspectionDeadline.isRecalculatedFromCompletion(),
                "La revisione deve ricalcolare in base all'esecuzione effettiva");
        assertFalse(inspectionDeadline.isCompleted());
    }

    @Test
    public void generateStandardInspectionDeadline_WithoutRegistrationDate_ThrowsException() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.NEW).build(); // Vehicle without a registration date

        assertThrows(IllegalStateException.class, () -> car.generateStandardInspectionDeadline(),
                "Cannot generate standard inspection deadlines without registration date");
    }

    @Test
    public void editCoreData_WhenAvailable_Success() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.SECONDHAND).build();

        assertDoesNotThrow(() -> {
            car.setBrand("Toyota");
            car.setModel("Yaris");
            car.setColor("Blu");
            car.setKilometers(Double.valueOf(6800));
        });
        assertEquals("Toyota", car.getBrand());
        assertEquals("Yaris", car.getModel());
        assertEquals("Blu", car.getColor());
        assertEquals(6800, car.getKilometers());
    }

    @Test
    public void editCoreData_WhenQuoted_ThrowsException() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.SECONDHAND).build();
        car.setStatus(VehicleStatus.QUOTED);

        assertThrows(IllegalStateException.class, () -> car.setBrand("Toyota"));
        assertThrows(IllegalStateException.class, () -> car.setModel("Yaris"));
        assertThrows(IllegalStateException.class, () -> car.setColor("Blu"));
        assertThrows(IllegalStateException.class, () -> car.setKilometers(Double.valueOf(6800)));
    }

    @Test
    public void editAnagraphicData_SecondHand_WhenQuoted_ThrowsException() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.SECONDHAND).build();
        car.setStatus(VehicleStatus.QUOTED);

        assertThrows(IllegalStateException.class, () -> car.setLicensePlate("AB123CD"),
                "Cannot edit license plate of a second-hand vehicle with status QUOTED");
        assertThrows(IllegalStateException.class, () -> car.setRegistrationDate(LocalDate.now()),
                "Cannot edit registration date of a second-hand vehicle with status QUOTED");
    }

    @Test
    public void editAnagraphicData_SecondHand_WhenReserved_ThrowsException() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.SECONDHAND).build();
        car.setStatus(VehicleStatus.RESERVED);

        assertThrows(IllegalStateException.class, () -> car.setLicensePlate("AB123CD"),
                "Cannot edit license plate of a second-hand vehicle with status RESERVED");
        assertThrows(IllegalStateException.class, () -> car.setRegistrationDate(LocalDate.now()),
                "Cannot edit registration date of a second-hand vehicle with status RESERVED");
    }

    @Test
    public void editAnagraphicData_New_WhenReserved_Success() {
        LocalDate today = LocalDate.now();
        Vehicle car = baseBuilder.setCondition(VehicleCondition.NEW).build();
        car.setStatus(VehicleStatus.RESERVED); // Auto nuova prenotata in attesa di immatricolazione

        assertDoesNotThrow(() -> {
            car.setLicensePlate("XX999YY");
            car.setRegistrationDate(today);
        });

        assertEquals("XX999YY", car.getLicensePlate());
        assertTrue(today.isEqual(car.getRegistrationDate()));
    }

    @Test
    public void editAnyData_WhenSold_ThrowsException() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.NEW).build();
        car.setStatus(VehicleStatus.SOLD);

        assertThrows(IllegalStateException.class, () -> car.setColor("Grigio"));
        assertThrows(IllegalStateException.class, () -> car.setLicensePlate("AB123CD"));
        assertThrows(IllegalStateException.class, () -> car.setHandoverDate(LocalDate.now()));
    }

    @Test
    public void editAnyData_WhenWithdrawn_ThrowsException() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.SECONDHAND).build();
        car.withdraw("Furto");

        assertThrows(IllegalStateException.class, () -> car.setColor("Grigio"));
        assertThrows(IllegalStateException.class, () -> car.setLicensePlate("AB123CD"));
        assertThrows(IllegalStateException.class, () -> car.setHandoverDate(LocalDate.now()));
        assertThrows(IllegalStateException.class, () -> car.setPurchaseTransaction(null));
    }

    @Test
    public void setStatus_ToTerminal_RemovesFromShowroom() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.NEW).build();

        assertTrue(car.isInShowroom());

        car.setStatus(VehicleStatus.SOLD);

        assertFalse(car.isInShowroom(), "When the vehicle is SOLD (handed over) it must be removed from the showroom");
    }

    @Test
    public void withdraw_RemovesFromShowroom() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.NEW).build();

        car.withdraw("Restituito al fornitore");

        assertFalse(car.isInShowroom(), "When the vehicle is WITHDRAWN it must be removed from the showroom");
        assertNotNull(car.getWithdrawalReason());
        assertEquals("Restituito al fornitore", car.getWithdrawalReason());
    }

    @Test
    public void withdraw_WithoutWithdrawalReason_ThrowsException() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.NEW).build();

        assertThrows(IllegalArgumentException.class, () -> car.withdraw(null));

    }

    @Test
    public void setStatus_ToWithdrawn_ThrowsException() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.NEW).build();

        assertThrows(IllegalArgumentException.class, () -> car.setStatus(VehicleStatus.WITHDRAWN));

    }
}