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
    public void isExpired_IncompleteEventWithDueDateInThePast_ReturnsTrue() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        assertTrue(deadline.isExpired(TODAY));
    }

    @Test
    public void isExpired_CompletedEventWithDueDateInThePast_ReturnsFalse() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        deadline.complete(TODAY, "Sostituite pastiglie");
        assertFalse(deadline.isExpired(TODAY));
    }

    @Test
    public void setters_IncompleteEvent_Success() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);

        assertAll(
                () -> assertDoesNotThrow(() -> deadline.setReason("Tagliando")),
                () -> assertEquals("Tagliando", deadline.getReason()),
                () -> assertDoesNotThrow(() -> deadline.setDueDate(TODAY)),
                () -> assertEquals(TODAY, deadline.getDueDate()),
                () -> assertDoesNotThrow(() -> deadline.setRecurrence(Period.ofYears(1))),
                () -> assertEquals(Period.ofYears(1), deadline.getRecurrence()),
                () -> assertDoesNotThrow(() -> deadline.setRecalculateFromCompletion(true)),
                () -> assertTrue(deadline.isRecalculatedFromCompletion()));
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
    public void setRecurrence_ToNull_WithRecalculateFromCompletion_SetsRecalculateFromCompletionToFalse() {
        Deadline deadline = new Deadline(defaultCar, "Tagliando", yesterday, Period.ofYears(1), true);
        assertNotNull(deadline.getRecurrence());
        assertTrue(deadline.isRecalculatedFromCompletion());

        deadline.setRecurrence(null);

        assertNull(deadline.getRecurrence());
        assertFalse(deadline.isRecalculatedFromCompletion());

    }

    @Test
    public void setRecalculateFromCompletion_ToTrue_WithNullRecurrence_ThrowsException() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", yesterday, null, false);
        assertNull(deadline.getRecurrence());
        assertFalse(deadline.isRecalculatedFromCompletion());

        assertThrows(IllegalArgumentException.class, () -> deadline.setRecalculateFromCompletion(true));
        assertNull(deadline.getRecurrence());
        assertFalse(deadline.isRecalculatedFromCompletion());

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