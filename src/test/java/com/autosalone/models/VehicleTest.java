package com.autosalone.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.autosalone.enums.TransactionType;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Vehicle.VehicleBuilder;

public class VehicleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);

    private VehicleBuilder baseBuilder;

    @BeforeEach
    public void setUp() {
        baseBuilder = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setSellingPrice(new BigDecimal("20000"))
                .setIsInShowroom(true);
    }

    @Test
    public void vehicleBuilder_CreatesVehicleInAvailableStatusAndNormalizesData() {
        Vehicle messyCar = new Vehicle.VehicleBuilder()
                .setBrand(" Fiat ")
                .setModel("  Panda ")
                .setColor(" Rosso ")
                .setSellingPrice(new BigDecimal("20000"))
                .setCondition(VehicleCondition.SECONDHAND)
                .setHandoverDate(TODAY)
                .setLicensePlate("Ab123cD ")
                .setRegistrationDate(TODAY)
                .setKilometers(Double.valueOf(68000))
                .build();

        assertAll(
                () -> assertEquals(VehicleStatus.AVAILABLE, messyCar.getStatus()),
                () -> assertFalse(messyCar.isInShowroom()),
                () -> assertEquals("Fiat", messyCar.getBrand()),
                () -> assertEquals("Panda", messyCar.getModel()),
                () -> assertEquals("Rosso", messyCar.getColor()),
                () -> assertEquals(0, messyCar.getSellingPrice().compareTo(new BigDecimal("20000"))),
                () -> assertEquals(TODAY, messyCar.getHandoverDate()),
                () -> assertEquals("AB123CD", messyCar.getLicensePlate()),
                () -> assertEquals(TODAY, messyCar.getRegistrationDate()),
                () -> assertEquals(0, messyCar.getKilometers().compareTo(Double.valueOf(68000))),
                () -> assertEquals(VehicleCondition.SECONDHAND, messyCar.getCondition()));
    }

    @Test
    public void vehicleBuilder_WithoutBrand_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Vehicle.VehicleBuilder()
                .setModel("Panda")
                .setColor("Rosso")
                .setSellingPrice(new BigDecimal("20000"))
                .setIsInShowroom(true)
                .setCondition(VehicleCondition.NEW)
                .build());
    }

    @Test
    public void vehicleBuilder_WithoutModel_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setColor("Rosso")
                .setSellingPrice(new BigDecimal("20000"))
                .setIsInShowroom(true)
                .setCondition(VehicleCondition.NEW)
                .build());
    }

    @Test
    public void vehicleBuilder_WithoutColor_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setSellingPrice(new BigDecimal("20000"))
                .setIsInShowroom(true)
                .setCondition(VehicleCondition.NEW)
                .build());
    }

    @Test
    public void vehicleBuilder_WithoutCondition_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setSellingPrice(new BigDecimal("20000"))
                .setIsInShowroom(true)
                .build());
    }

    @Test
    public void vehicleBuilder_WithoutSellingPrice_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setIsInShowroom(true)
                .setCondition(VehicleCondition.NEW)
                .build());
    }

    @Test
    public void vehicleBuilder_WithNegativeSellingPrice_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setSellingPrice(new BigDecimal("-20000"))
                .setIsInShowroom(true)
                .setCondition(VehicleCondition.NEW)
                .build());
    }

    @Test
    public void setSellingPrice_ToNull_ThrowsException() {
        Vehicle car = getNewCar();
        assertThrows(IllegalArgumentException.class, () -> car.setSellingPrice(null));
    }

    @Test
    public void setSellingPrice_ToNegative_ThrowsException() {
        Vehicle car = getNewCar();
        assertThrows(IllegalArgumentException.class, () -> car.setSellingPrice(new BigDecimal("-20000")));
    }

    @Test
    public void setSellingPrice_ToPositive_Success() {
        Vehicle car = getNewCar();
        assertDoesNotThrow(() -> car.setSellingPrice(new BigDecimal("30000")));
        assertEquals(0, car.getSellingPrice().compareTo(new BigDecimal("30000")));
    }

    @Test
    public void generateStandardInspectionDeadline_SetsInspectionTo4YearsEndOfMonth() {
        Vehicle car = baseBuilder.setCondition(VehicleCondition.SECONDHAND)
                .setRegistrationDate(TODAY)
                .build();

        Deadline inspectionDeadline = car.generateStandardInspectionDeadline(TODAY);

        assertEquals(1, car.getDeadlines().size());

        LocalDate expectedDate = TODAY.plusYears(4)
                .with(TemporalAdjusters.lastDayOfMonth());

        assertEquals("Revisione Veicolo", inspectionDeadline.getReason());
        assertTrue(expectedDate.isEqual(inspectionDeadline.getDueDate()));
        assertEquals(2, inspectionDeadline.getRecurrence().getYears());
        assertTrue(inspectionDeadline.isRecalculatedFromCompletion(),
                "The next due inspection must be recalculated from the actual completion");
        assertFalse(inspectionDeadline.isCompleted());
    }

    @Test
    public void generateStandardInspectionDeadline_WithoutRegistrationDate_ThrowsException() {
        Vehicle car = getNewCar(); // Vehicle without a registration date
        assertThrows(IllegalStateException.class, () -> car.generateStandardInspectionDeadline(TODAY),
                "Cannot generate standard inspection deadlines without registration date");
    }

    @Test
    public void generateInspectionFromLastDate_SetsInspectionTo2YearsEndOfMonth() {
        Vehicle car = getSecondHandCar();
        Deadline inspectionDeadline = car.generateInspectionFromLastDate(TODAY);

        assertEquals(1, car.getDeadlines().size());

        LocalDate expectedDate = TODAY.plusYears(2)
                .with(TemporalAdjusters.lastDayOfMonth());

        assertEquals("Revisione Veicolo", inspectionDeadline.getReason());
        assertTrue(expectedDate.isEqual(inspectionDeadline.getDueDate()));
        assertEquals(2, inspectionDeadline.getRecurrence().getYears());
        assertTrue(inspectionDeadline.isRecalculatedFromCompletion(),
                "The next due inspection must be recalculated from the actual completion");
        assertFalse(inspectionDeadline.isCompleted());
    }

    @Test
    public void generateInspectionFromLastDate_FromNullDate_ThrowsException() {
        Vehicle car = getSecondHandCar();
        assertThrows(IllegalArgumentException.class, () -> car.generateInspectionFromLastDate(null));
    }

    @Test
    public void editCoreData_WhenAvailable_NormalizesDataSuccess() {
        Vehicle car = getSecondHandCar();

        assertDoesNotThrow(() -> {
            car.setBrand(" Toyota  ");
            car.setModel("  Yaris ");
            car.setColor(" Blu ");
            car.setKilometers(Double.valueOf(6800));
            car.setIsInShowroom(false);
            car.setCondition(VehicleCondition.NEW);
        });
        assertAll(
                () -> assertEquals("Toyota", car.getBrand()),
                () -> assertEquals("Yaris", car.getModel()),
                () -> assertEquals("Blu", car.getColor()),
                () -> assertEquals(0, car.getKilometers().compareTo(Double.valueOf(6800))),
                () -> assertFalse(car.isInShowroom()),
                () -> assertEquals(VehicleCondition.NEW, car.getCondition()));
    }

    @Test
    public void setBrand_Blank_WhenAvailable_ThrowsException() {
        Vehicle car = getSecondHandCar();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> car.setBrand("  "));
        assertEquals("Brand is required", exception.getMessage());
    }

    @Test
    public void setBrand_ToNull_WhenAvailable_ThrowsException() {
        Vehicle car = getSecondHandCar();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> car.setBrand(null));
        assertEquals("Brand is required", exception.getMessage());
    }

    @Test
    public void setModel_Blank_WhenAvailable_ThrowsException() {
        Vehicle car = getSecondHandCar();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> car.setModel("  "));
        assertEquals("Model is required", exception.getMessage());
    }

    @Test
    public void setModel_ToNull_WhenAvailable_ThrowsException() {
        Vehicle car = getSecondHandCar();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> car.setModel(null));
        assertEquals("Model is required", exception.getMessage());
    }

    @Test
    public void setColor_Blank_WhenAvailable_ThrowsException() {
        Vehicle car = getSecondHandCar();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> car.setColor("  "));
        assertEquals("Color is required", exception.getMessage());
    }

    @Test
    public void setColor_ToNull_WhenAvailable_ThrowsException() {
        Vehicle car = getSecondHandCar();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> car.setColor(null));
        assertEquals("Color is required", exception.getMessage());
    }

    @Test
    public void setCondition_ToNull_WhenAvailable_ThrowsException() {
        Vehicle car = getSecondHandCar();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> car.setCondition(null));
        assertEquals("Condition is required", exception.getMessage());
    }

    @Test
    public void setKilometers_ToNegative_WhenAvailable_ThrowsException() {
        Vehicle car = getSecondHandCar();
        assertThrows(IllegalArgumentException.class, () -> car.setKilometers(Double.valueOf(-30000)));
    }

    @Test
    public void setLicensePlate_Blank_WhenAvailable_SetsNull() {
        Vehicle car = getSecondHandCar();
        car.setLicensePlate("  ");
        assertNull(car.getLicensePlate());
    }

    @Test
    public void setLicensePlate_ToNull_WhenAvailable_SetsNull() {
        Vehicle car = getSecondHandCar();
        car.setLicensePlate(null);
        assertNull(car.getLicensePlate());
    }

    @Test
    public void setLicensePlate_WhenAvailable_NormalizesValue() {
        Vehicle car = getSecondHandCar();
        car.setLicensePlate("Ab123cD ");
        assertEquals("AB123CD", car.getLicensePlate());
    }

    @Test
    public void editCoreData_WhenQuoted_ThrowsException() {
        Vehicle car = getSecondHandCar();
        car.setStatus(VehicleStatus.QUOTED);

        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> car.setBrand("Toyota")),
                () -> assertThrows(IllegalStateException.class, () -> car.setModel("Yaris")),
                () -> assertThrows(IllegalStateException.class, () -> car.setColor("Blu")),
                () -> assertThrows(IllegalStateException.class, () -> car.setKilometers(Double.valueOf(6800))));
    }

    @Test
    public void editAnagraphicData_SecondHand_WhenQuoted_ThrowsException() {
        Vehicle car = getSecondHandCar();
        car.setStatus(VehicleStatus.QUOTED);

        assertThrows(IllegalStateException.class, () -> car.setLicensePlate("AB123CD"),
                "Cannot edit license plate of a second-hand vehicle with status QUOTED");
        assertThrows(IllegalStateException.class, () -> car.setRegistrationDate(TODAY),
                "Cannot edit registration date of a second-hand vehicle with status QUOTED");
    }

    @Test
    public void editAnagraphicData_SecondHand_WhenReserved_ThrowsException() {
        Vehicle car = getSecondHandCar();
        car.setStatus(VehicleStatus.RESERVED);

        assertThrows(IllegalStateException.class, () -> car.setLicensePlate("AB123CD"),
                "Cannot edit license plate of a second-hand vehicle with status RESERVED");
        assertThrows(IllegalStateException.class, () -> car.setRegistrationDate(TODAY),
                "Cannot edit registration date of a second-hand vehicle with status RESERVED");
    }

    @Test
    public void editAnagraphicData_New_WhenReserved_Success() {
        Vehicle car = getNewCar();
        car.setStatus(VehicleStatus.RESERVED); // Auto nuova prenotata in attesa di immatricolazione

        assertDoesNotThrow(() -> {
            car.setLicensePlate("XX999YY");
            car.setRegistrationDate(TODAY);
        });

        assertEquals("XX999YY", car.getLicensePlate());
        assertTrue(TODAY.isEqual(car.getRegistrationDate()));
    }

    @Test
    public void editAnyData_WhenSold_ThrowsException() {
        Vehicle car = getNewCar();
        car.setStatus(VehicleStatus.SOLD);

        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> car.setColor("Grigio")),
                () -> assertThrows(IllegalStateException.class, () -> car.setLicensePlate("AB123CD")),
                () -> assertThrows(IllegalStateException.class, () -> car.setHandoverDate(TODAY)));
    }

    @Test
    public void editAnyData_WhenWithdrawn_ThrowsException() {
        Vehicle car = getSecondHandCar();
        car.withdraw("Furto");
        Transaction purchase = new Transaction("Acquisto", new BigDecimal("8000.00"), TODAY.minusDays(12),
                TransactionType.OUT);

        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> car.setColor("Grigio")),
                () -> assertThrows(IllegalStateException.class, () -> car.setLicensePlate("AB123CD")),
                () -> assertThrows(IllegalStateException.class, () -> car.setHandoverDate(TODAY)),
                () -> assertThrows(IllegalStateException.class, () -> car.setPurchaseTransaction(purchase)));
    }

    @Test
    public void setStatus_ToTerminal_RemovesFromShowroom() {
        Vehicle car = getNewCar();

        assertTrue(car.isInShowroom());

        car.setStatus(VehicleStatus.SOLD);

        assertFalse(car.isInShowroom(), "When the vehicle is SOLD (handed over) it must be removed from the showroom");
    }

    @Test
    public void addExpense_WhenSold_ThrowsException() {
        Vehicle car = getNewCar();
        Transaction expense = new Transaction("Spesa", new BigDecimal("100"), TODAY, TransactionType.OUT);
        car.setStatus(VehicleStatus.SOLD);

        assertThrows(IllegalStateException.class, () -> car.addExpense(expense));
    }

    @Test
    public void addExpense_WhenWithdrawn_ThrowsException() {
        Vehicle car = getNewCar();
        Transaction expense = new Transaction("Spesa", new BigDecimal("100"), TODAY, TransactionType.OUT);
        car.withdraw("Ritiro");

        assertThrows(IllegalStateException.class, () -> car.addExpense(expense));
    }

    @Test
    public void addExpense_WhenAvailable_Success() {
        Vehicle car = getNewCar();
        Transaction expense = new Transaction("Spesa", new BigDecimal("100"), TODAY, TransactionType.OUT);

        assertDoesNotThrow(() -> car.addExpense(expense));
        assertEquals(1, car.getExpenses().size());
    }

    @Test
    public void withdraw_RemovesFromShowroomAndNormalizesReason() {
        Vehicle car = getNewCar();

        car.withdraw(" Restituito al fornitore  ");

        assertFalse(car.isInShowroom(), "When the vehicle is WITHDRAWN it must be removed from the showroom");
        assertNotNull(car.getWithdrawalReason());
        assertEquals("Restituito al fornitore", car.getWithdrawalReason());
    }

    @Test
    public void withdraw_WithoutWithdrawalReason_ThrowsException() {
        Vehicle car = getNewCar();
        assertThrows(IllegalArgumentException.class, () -> car.withdraw(null));
        assertEquals(VehicleStatus.AVAILABLE, car.getStatus());
        assertTrue(car.isInShowroom());
    }

    @Test
    public void setStatus_ToWithdrawn_ThrowsException() {
        Vehicle car = getNewCar();
        assertThrows(IllegalArgumentException.class, () -> car.setStatus(VehicleStatus.WITHDRAWN));
        assertEquals(VehicleStatus.AVAILABLE, car.getStatus());
    }

    @Test
    public void setPurchaseTransaction_AlreadyRegistered_ThrowsException() {
        Vehicle car = getSecondHandCar();
        Transaction purchase = new Transaction("Acquisto", new BigDecimal(18000), TODAY, TransactionType.OUT);
        Transaction otherPurchase = new Transaction("Acquisto", new BigDecimal(18000), TODAY, TransactionType.OUT);
        car.setPurchaseTransaction(purchase);
        assertThrows(IllegalStateException.class, () -> car.setPurchaseTransaction(otherPurchase),
                "Cannot override purchase transaction");
    }

    private Vehicle getSecondHandCar() {
        return baseBuilder.setCondition(VehicleCondition.SECONDHAND).build();
    }

    private Vehicle getNewCar() {
        return baseBuilder.setCondition(VehicleCondition.NEW).build();
    }
}