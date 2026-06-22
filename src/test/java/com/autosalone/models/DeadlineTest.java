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
    public void complete_SingleEvent_ReturnsNull() {
        // scadenza singola, non si ripete
        Deadline deadline = new Deadline("Riparazione freni", LocalDate.of(2025, 5, 10), null, false, defaultCar);

        Deadline nextEvent = deadline.complete(LocalDate.of(2025, 5, 8), "Sostituite pastiglie");

        assertTrue(deadline.isCompleted(), "The deadline should have been completed");
        assertNull(nextEvent, "Without a recurrence it must not generate a next deadline event");
    }

    @Test
    public void complete_RecalculateFromCompletion_GeneratesCorrectNextEvent() {
        // tagliando: ogni anno, ricalcola dall'effettiva data di completamento
        Deadline deadline = new Deadline("Tagliando", LocalDate.of(2025, 10, 1), Period.ofYears(1), true, defaultCar);

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
        Deadline deadline = new Deadline("Bollo Auto", LocalDate.of(2025, 8, 31), Period.ofYears(1), false, defaultCar);

        // completato in anticipo
        LocalDate completionDate = LocalDate.of(2025, 6, 10);
        Deadline nextEvent = deadline.complete(completionDate, "Pagato tramite PagoPA");

        assertNotNull(nextEvent, "With a recurrence it must generate the next deadline event");
        assertTrue(LocalDate.of(2026, 8, 31).isEqual(nextEvent.getDueDate()),
                "The next deadline should be in a year from the due date of the last one");
    }

    @Test
    public void complete_AlreadyCompleted_ThrowsException() {
        Deadline deadline = new Deadline("Riparazione", LocalDate.now(), null, false, defaultCar);
        deadline.complete(LocalDate.now(), "Fatto");

        assertThrows(IllegalStateException.class, () -> {
            deadline.complete(LocalDate.now(), null);
        }, "Cannot complete a completed deadline event");
    }
}