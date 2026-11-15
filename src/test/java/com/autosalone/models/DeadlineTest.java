package com.autosalone.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.autosalone.enums.VehicleCondition;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

public class DeadlineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);
    private LocalDate yesterday;

    private Vehicle defaultCar;

    @BeforeEach
    public void setUp() {
        this.defaultCar = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setIsInShowroom(true)
                .setCondition(VehicleCondition.SECONDHAND)
                .setSellingPrice(new BigDecimal("10000.00"))
                .build();
        this.yesterday = TODAY.minusDays(1);
    }

    @Test
    public void constructor_RecalculateFromCompletionWithoutRecurrence_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Deadline(defaultCar, "Tagliando", TODAY, null, true),
                "Cannot recalculate from completion if there is no recurrence");
    }

    @Test
    public void constructor_WithoutReason_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Deadline(defaultCar, null, TODAY, null, false));
        assertEquals("Reason is required", exception.getMessage());
    }

    @Test
    public void constructor_WithoutDueDate_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Deadline(defaultCar, "Tagliando", null, null, false));
        assertEquals("Due date is required", exception.getMessage());
    }

    @Test
    public void constructor_WithoutVehicle_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Deadline(null, "Tagliando", TODAY, null, false));
        assertEquals("A deadline must be linked to a vehicle", exception.getMessage());
    }

    @Test
    public void constructor_WithValidData_AssignsAndNormalizesData() {
        Deadline deadline = new Deadline(defaultCar, "  Tagliando ", TODAY, Period.ofYears(1), true);

        assertAll(
                () -> assertEquals(defaultCar, deadline.getVehicle()),
                () -> assertEquals("Tagliando", deadline.getReason()),
                () -> assertEquals(TODAY, deadline.getDueDate()),
                () -> assertEquals(Period.ofYears(1), deadline.getRecurrence()),
                () -> assertTrue(deadline.isRecalculatedFromCompletion()));
    }

    @Test
    public void isExpired_IncompleteEventWithDueDateInThePast_ReturnsTrue() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        assertTrue(deadline.isExpired(TODAY));
    }

    @Test
    public void isExpired_IncompleteEventWithDueDateInTheFuture_ReturnsFalse() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", TODAY.plusDays(1), null, false);
        assertFalse(deadline.isExpired(TODAY));
    }

    @Test
    public void isExpired_CompletedEventWithDueDateInThePast_ReturnsFalse() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        deadline.complete(TODAY, "Sostituite pastiglie");
        assertFalse(deadline.isExpired(TODAY));
    }

    @Test
    public void setReason_Blank_IncompleteEvent_ThrowsException() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> deadline.setReason(" "));
        assertEquals("Reason is required", exception.getMessage());
        assertEquals("Riparazione freni", deadline.getReason());
    }

    @Test
    public void setReason_ToNull_IncompleteEvent_ThrowsException() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> deadline.setReason(null));
        assertEquals("Reason is required", exception.getMessage());
        assertEquals("Riparazione freni", deadline.getReason());
    }

    @Test
    public void setReason_IncompleteEvent_NormalizesValue() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        deadline.setReason(" Tagliando  ");
        assertEquals("Tagliando", deadline.getReason());
    }

    @Test
    public void setDueDate_ToNull_IncompleteEvent_ThrowsException() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> deadline.setDueDate(null));
        assertEquals("Due date is required", exception.getMessage());
        assertEquals(yesterday, deadline.getDueDate());
    }

    @Test
    public void setDueDate_IncompleteEvent_Success() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        deadline.setDueDate(TODAY);
        assertEquals(TODAY, deadline.getDueDate());
    }

    @Test
    public void setRecurrence_IncompleteEvent_Success() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        deadline.setRecurrence(Period.ofYears(1));
        assertEquals(Period.ofYears(1), deadline.getRecurrence());
    }

    @Test
    public void setRecurrence_ToNull_WithRecalculateFromCompletion_IncompleteEvent_SetsRecalculateFromCompletionToFalse() {
        Deadline deadline = new Deadline(defaultCar, "Tagliando", yesterday, Period.ofYears(1), true);
        deadline.setRecurrence(null);
        assertNull(deadline.getRecurrence());
        assertFalse(deadline.isRecalculatedFromCompletion());
    }

    @Test
    public void setRecalculateFromCompletion_ToTrue_IncompleteEvent_Success() {
        Deadline deadline = new Deadline(defaultCar, "Tagliando", yesterday, Period.ofYears(1), false);
        deadline.setRecalculateFromCompletion(true);
        assertTrue(deadline.isRecalculatedFromCompletion());
    }

    @Test
    public void setRecalculateFromCompletion_ToTrue_WithNullRecurrence_IncompleteEvent_ThrowsException() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        assertThrows(IllegalArgumentException.class, () -> deadline.setRecalculateFromCompletion(true));
        assertNull(deadline.getRecurrence());
        assertFalse(deadline.isRecalculatedFromCompletion());
    }

    @Test
    public void setters_CompletedEvent_ThrowsException() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        deadline.complete(TODAY, "Sostituite pastiglie");

        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> deadline.setReason("Tagliando")),
                () -> assertThrows(IllegalStateException.class, () -> deadline.setDueDate(TODAY)),
                () -> assertThrows(IllegalStateException.class, () -> deadline.setRecurrence(Period.ofYears(1))),
                () -> assertThrows(IllegalStateException.class, () -> deadline.setRecalculateFromCompletion(true)));
    }

    @Test
    public void complete_SingleEvent_ReturnsNull() {
        // scadenza singola, non si ripete
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);

        Deadline nextEvent = deadline.complete(TODAY, "Sostituite pastiglie");

        assertTrue(deadline.isCompleted(), "The deadline should have been completed");
        assertEquals(TODAY, deadline.getCompletionDate());
        assertEquals("Sostituite pastiglie", deadline.getNotes());
        assertNull(nextEvent, "Without a recurrence it must not generate a next deadline event");
    }

    @Test
    public void complete_RecalculateFromCompletion_GeneratesCorrectNextEvent() {
        // tagliando: ogni anno, ricalcola dall'effettiva data di completamento
        Deadline deadline = new Deadline(defaultCar, "Tagliando", TODAY, Period.ofYears(1), true);

        // completato in anticipo
        Deadline nextEvent = deadline.complete(yesterday, "Cambio olio e filtri, speso 300€");

        assertNotNull(nextEvent, "With a recurrence it must generate the next deadline event");
        assertEquals("Tagliando", nextEvent.getReason());
        assertTrue(yesterday.plusYears(1).isEqual(nextEvent.getDueDate()),
                "The next deadline should be in a year from the completion date of the last one");
        assertFalse(nextEvent.isCompleted());
    }

    @Test
    public void complete_DoNotRecalculate_GeneratesCorrectNextEvent() {
        // bollo: ogni anno, NON ricalcola dalla data effettiva di completamento
        Deadline deadline = new Deadline(defaultCar, "Bollo Auto", TODAY, Period.ofYears(1), false);

        // completato in anticipo
        Deadline nextEvent = deadline.complete(yesterday, "Pagato tramite PagoPA");

        assertNotNull(nextEvent, "With a recurrence it must generate the next deadline event");
        assertTrue(TODAY.plusYears(1).isEqual(nextEvent.getDueDate()),
                "The next deadline should be in a year from the due date of the last one");
    }

    @Test
    public void complete_AlreadyCompleted_ThrowsException() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione", TODAY, null, false);
        deadline.complete(TODAY, "Fatto");

        assertThrows(IllegalStateException.class, () -> {
            deadline.complete(TODAY, null);
        }, "Cannot complete a completed deadline event");
    }

    @Test
    public void complete_WithoutActualCompletionDate_ThrowsException() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione", TODAY, null, false);

        assertThrows(IllegalArgumentException.class, () -> {
            deadline.complete(null, "Fatto");
        });
    }

    @Test
    public void complete_WithBlankNotes_SetsNullNotes() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione", TODAY, null, false);

        deadline.complete(TODAY, "  ");
        assertNull(deadline.getNotes());
    }

    @Test
    public void complete_WithNullNotes_SetsNullNotes() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione", TODAY, null, false);

        deadline.complete(TODAY, null);
        assertNull(deadline.getNotes());
    }

    @Test
    public void complete_WithNotes_NormalizesValue() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione", TODAY, null, false);

        deadline.complete(TODAY, " Fatto  ");
        assertEquals("Fatto", deadline.getNotes());
    }

    @Test
    public void complete_VehicleInspection_GeneratesCorrectNextEvent() {
        Vehicle car = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setIsInShowroom(true)
                .setSellingPrice(new BigDecimal("20000"))
                .setCondition(VehicleCondition.SECONDHAND)
                .build();

        Deadline inspectionDeadline = car.generateInspectionFromLastDate(LocalDate.of(2025, 10, 12));
        assertEquals(car, inspectionDeadline.getVehicle());

        LocalDate completionDate = LocalDate.of(2026, 5, 15);
        Deadline nextEvent = inspectionDeadline.complete(completionDate, "Cambio olio e filtri, speso 300€");

        assertNotNull(nextEvent, "With a recurrence it must generate the next deadline event");
        assertEquals(Deadline.VEHICLE_INSPECTION_REASON, nextEvent.getReason());
        assertTrue(LocalDate.of(2028, 5, 31).isEqual(nextEvent.getDueDate()),
                "The next inspection deadline should be in two years from the completion date of the last one and on the last day of the month");
        assertFalse(nextEvent.isCompleted());
    }
}