package com.autosalone.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.autosalone.enums.VehicleCondition;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

public class DeadlineTest {

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
    }

    @Test
    public void constructor_RecalculateFromCompletionWithoutRecurrence_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Deadline(defaultCar, "Tagliando", LocalDate.of(2025, 10, 1), null, true),
                "Cannot recalculate from completion if there is no recurrence");
    }

    @Test
    public void isExpired_IncompleteEventWithDueDateInThePast_ReturnsTrue() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", LocalDate.of(2025, 5, 10), null, false);
        assertTrue(deadline.isExpired());
    }

    @Test
    public void isExpired_CompletedEventWithDueDateInThePast_ReturnsFalse() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", LocalDate.of(2025, 5, 10), null, false);
        deadline.complete(LocalDate.now(), "Sostituite pastiglie");
        assertFalse(deadline.isExpired());
    }

    @Test
    public void setters_IncompleteEvent_Success() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", LocalDate.of(2025, 5, 10), null, false);

        assertAll(
                () -> assertDoesNotThrow(() -> deadline.setReason("Tagliando")),
                () -> assertEquals("Tagliando", deadline.getReason()),
                () -> assertDoesNotThrow(() -> deadline.setDueDate(LocalDate.of(2025, 10, 1))),
                () -> assertTrue(LocalDate.of(2025, 10, 1).equals(deadline.getDueDate())),
                () -> assertDoesNotThrow(() -> deadline.setRecurrence(Period.ofYears(1))),
                () -> assertTrue(Period.ofYears(1).equals(deadline.getRecurrence())),
                () -> assertDoesNotThrow(() -> deadline.setRecalculateFromCompletion(true)),
                () -> assertTrue(deadline.isRecalculatedFromCompletion()));
    }

    @Test
    public void setters_CompletedEvent_ThrowsException() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", LocalDate.of(2025, 5, 10), null, false);
        deadline.complete(LocalDate.now(), "Sostituite pastiglie");

        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> deadline.setReason("Tagliando")),
                () -> assertThrows(IllegalStateException.class, () -> deadline.setDueDate(LocalDate.of(2025, 10, 1))),
                () -> assertThrows(IllegalStateException.class, () -> deadline.setRecurrence(Period.ofYears(1))),
                () -> assertThrows(IllegalStateException.class, () -> deadline.setRecalculateFromCompletion(true)));
    }

    @Test
    public void setRecurrence_ToNull_WithRecalculateFromCompletion_SetsRecalculateFromCompletionToFalse() {
        Deadline deadline = new Deadline(defaultCar, "Tagliando", LocalDate.of(2025, 10, 1), Period.ofYears(1), true);
        assertNotNull(deadline.getRecurrence());
        assertTrue(deadline.isRecalculatedFromCompletion());

        deadline.setRecurrence(null);

        assertNull(deadline.getRecurrence());
        assertFalse(deadline.isRecalculatedFromCompletion());

    }

    @Test
    public void setRecalculateFromCompletion_ToTrue_WithNullRecurrence_ThrowsException() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", LocalDate.of(2025, 5, 10), null, false);
        assertNull(deadline.getRecurrence());
        assertFalse(deadline.isRecalculatedFromCompletion());

        assertThrows(IllegalArgumentException.class, () -> deadline.setRecalculateFromCompletion(true));
        assertNull(deadline.getRecurrence());
        assertFalse(deadline.isRecalculatedFromCompletion());

    }

    @Test
    public void complete_SingleEvent_ReturnsNull() {
        // scadenza singola, non si ripete
        Deadline deadline = new Deadline(defaultCar, "Riparazione freni", LocalDate.of(2025, 5, 10), null, false);

        Deadline nextEvent = deadline.complete(LocalDate.of(2025, 5, 8), "Sostituite pastiglie");

        assertTrue(deadline.isCompleted(), "The deadline should have been completed");
        assertTrue(LocalDate.of(2025, 5, 8).equals(deadline.getCompletionDate()));
        assertEquals("Sostituite pastiglie", deadline.getNotes());
        assertNull(nextEvent, "Without a recurrence it must not generate a next deadline event");
    }

    @Test
    public void complete_RecalculateFromCompletion_GeneratesCorrectNextEvent() {
        // tagliando: ogni anno, ricalcola dall'effettiva data di completamento
        Deadline deadline = new Deadline(defaultCar, "Tagliando", LocalDate.of(2025, 10, 1), Period.ofYears(1), true);

        // completato in anticipo
        LocalDate completionDate = LocalDate.of(2025, 5, 15);
        Deadline nextEvent = deadline.complete(completionDate, "Cambio olio e filtri, speso 300€");

        assertNotNull(nextEvent, "With a recurrence it must generate the next deadline event");
        assertEquals("Tagliando", nextEvent.getReason());
        assertTrue(LocalDate.of(2026, 5, 15).isEqual(nextEvent.getDueDate()),
                "The next deadline should be in a year from the completion date of the last one");
        assertFalse(nextEvent.isCompleted());
    }

    @Test
    public void complete_DoNotRecalculate_GeneratesCorrectNextEvent() {
        // bollo: ogni anno, NON ricalcola
        Deadline deadline = new Deadline(defaultCar, "Bollo Auto", LocalDate.of(2025, 8, 31), Period.ofYears(1), false);

        // completato in anticipo
        LocalDate completionDate = LocalDate.of(2025, 6, 10);
        Deadline nextEvent = deadline.complete(completionDate, "Pagato tramite PagoPA");

        assertNotNull(nextEvent, "With a recurrence it must generate the next deadline event");
        assertTrue(LocalDate.of(2026, 8, 31).isEqual(nextEvent.getDueDate()),
                "The next deadline should be in a year from the due date of the last one");
    }

    @Test
    public void complete_AlreadyCompleted_ThrowsException() {
        Deadline deadline = new Deadline(defaultCar, "Riparazione", LocalDate.now(), null, false);
        deadline.complete(LocalDate.now(), "Fatto");

        assertThrows(IllegalStateException.class, () -> {
            deadline.complete(LocalDate.now(), null);
        }, "Cannot complete a completed deadline event");
    }

    @Test
    public void complete_VehicleInspection_GeneratesCorrectNextEvent() {
        Vehicle car = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setIsInShowroom(true)
                .setCondition(VehicleCondition.SECONDHAND)
                .build();

        car.generateInspectionFromLastDate(LocalDate.of(2025, 10, 12));
        Deadline deadline = car.getDeadlines().getFirst();
        assertEquals(car, deadline.getVehicle());

        LocalDate completionDate = LocalDate.of(2026, 5, 15);
        Deadline nextEvent = deadline.complete(completionDate, "Cambio olio e filtri, speso 300€");

        assertNotNull(nextEvent, "With a recurrence it must generate the next deadline event");
        assertEquals(Deadline.VEHICLE_INSPECTION_REASON, nextEvent.getReason());
        assertTrue(LocalDate.of(2028, 5, 31).isEqual(nextEvent.getDueDate()),
                "The next inspection deadline should be in two years from the completion date of the last one and on the last day of the month");
        assertFalse(nextEvent.isCompleted());
    }
}