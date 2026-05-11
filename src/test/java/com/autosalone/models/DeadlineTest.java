package com.autosalone.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.Period;

public class DeadlineTest {

    @Test
    public void getNextDate_StartDateIsRecentPast_ReturnsFirstRecurrence() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(6);
        Period recurrence = Period.ofYears(1);

        Deadline deadline = new Deadline(startDate, "Manutenzione annuale con inizio nel passato", recurrence, null);
        LocalDate nextDate = deadline.getNextDate();

        LocalDate expectedDate = startDate.plus(recurrence);

        assertEquals(expectedDate, nextDate, "Next date should be the first recurrence after today");
        assertTrue(nextDate.isAfter(today) || nextDate.isEqual(today), "Next date must not be in the past");
    }

    @Test
    public void getNextDate_StartDateIsToday_ReturnsToday() {
        LocalDate today = LocalDate.now();
        Period recurrence = Period.ofYears(1);

        Deadline deadline = new Deadline(today, "Inspection Due Today", recurrence, null);

        LocalDate nextDate = deadline.getNextDate();

        assertEquals(today,
                nextDate, "If the start date is exactly today, Next date must be today without adding recurrence");
    }

    @Test
    public void getNextDate_StartDateIsInFuture_ReturnsStartDate() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.plusMonths(1);
        Period recurrence = Period.ofYears(1);

        Deadline deadline = new Deadline(startDate, "Manutenzione annuale con inizio nel futuro", recurrence, null);

        LocalDate nextDate = deadline.getNextDate();

        assertEquals(startDate,
                nextDate, "If the start date is in the future, it should be considered the very first deadline");
        assertTrue(nextDate.isAfter(today) || nextDate.isEqual(today), "Next date must not be in the past");
    }

    @Test
    public void getNextDate_StartDateIsLongAgo_LoopsMultipleTimesToFindFutureDate() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusYears(5).minusMonths(1);
        Period recurrence = Period.ofYears(2);

        Deadline deadline = new Deadline(startDate, "Revisione biennale iniziata nel passato", recurrence, null);

        LocalDate nextDate = deadline.getNextDate();

        LocalDate expectedDate = startDate.plusYears(6);

        assertEquals(expectedDate, nextDate, "The loop should have executed 3 times to find the future date");
        assertTrue(nextDate.isAfter(today), "Next date must be strictly in the future");
    }

    @Test
    public void getNextDate_BeforeEndDate_ReturnsValidDate() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusYears(1);
        Period recurrence = Period.ofYears(1);
        LocalDate endDate = today.plusYears(5);

        Deadline deadline = new Deadline(startDate, "Assicurazione", recurrence, endDate);

        LocalDate nextDate = deadline.getNextDate();

        assertNotNull(nextDate, "Next date must not be null, since it is before the End date");
        assertEquals(startDate.plusYears(1), nextDate);
    }

    @Test
    public void getNextDate_AfterEndDate_ReturnsNull() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusYears(5);
        Period recurrence = Period.ofYears(1);
        LocalDate endDate = today.minusYears(1);

        Deadline deadline = new Deadline(startDate, "Finanziamento", recurrence, endDate);

        LocalDate nextDate = deadline.getNextDate();

        assertNull(nextDate, "Next date must be null, since it is after the End date");
    }

    @Test
    public void getNextDate_NullRecurrenceInFuture_ReturnsStartDate() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.plusDays(10);
        
        Deadline deadline = new Deadline(startDate, "Scadenza senza ricorrenza nel futuro", null, null);

        LocalDate nextDate = deadline.getNextDate();

        assertEquals(startDate, nextDate, "A one-off future deadline should return its start date");
    }

    @Test
    public void getNextDate_NullRecurrenceInPast_ReturnsNull() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(5);
        
        Deadline deadline = new Deadline(startDate, "Scadenza senza ricorrenza nel passato", null, null);

        LocalDate nextDate = deadline.getNextDate();

        assertNull(nextDate, "A one-off past deadline should return null, since it does not repeat");
    }

    @Test
    public void constructor_EndDateBeforeStartDate_ThrowsIllegalArgumentException() {
        LocalDate startDate = LocalDate.now();
        LocalDate invalidEndDate = startDate.minusDays(5);
        Period recurrence = Period.ofYears(1);

        assertThrows(IllegalArgumentException.class, () -> {
            new Deadline(startDate, "Scadenza non valida", recurrence, invalidEndDate);
        });
    }

    @Test
    public void constructor_EndDateEqualsStartDate_ThrowsIllegalArgumentException() {
        LocalDate startDate = LocalDate.now();
        LocalDate invalidEndDate = startDate;
        Period recurrence = Period.ofMonths(1);

        assertThrows(IllegalArgumentException.class, () -> {
            new Deadline(startDate, "Scadenza non valida", recurrence, invalidEndDate);
        }, "Should throw an exception if start and end dates are the same day");
    }

    @Test
    public void setEndDate_BeforeExistingStartDate_ThrowsIllegalArgumentException() {
        LocalDate startDate = LocalDate.now();
        LocalDate validEndDate = startDate.plusYears(2);
        Deadline deadline = new Deadline(startDate, "Scadenza valida", Period.ofYears(1), validEndDate);

        LocalDate invalidNewEndDate = startDate.minusDays(1);
        
        assertThrows(IllegalArgumentException.class, () -> {
            deadline.setEndDate(invalidNewEndDate);
        }, "Setter should prevent changing the end date to before the start date");
    }

    @Test
    public void setStartDate_AfterExistingEndDate_ThrowsIllegalArgumentException() {
        LocalDate startDate = LocalDate.now();
        LocalDate validEndDate = startDate.plusYears(1);
        Deadline deadline = new Deadline(startDate, "Scadenza valida", Period.ofYears(1), validEndDate);

        LocalDate invalidNewStartDate = validEndDate.plusDays(10);
        
        assertThrows(IllegalArgumentException.class, () -> {
            deadline.setStartDate(invalidNewStartDate);
        }, "Setter should prevent changing the start date to after the end date");
    }
}