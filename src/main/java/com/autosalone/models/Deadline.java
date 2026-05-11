package com.autosalone.models;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

public class Deadline {
    private final UUID id;
    private LocalDate startDate;
    private String reason;
    private Period recurrence;
    private LocalDate endDate;

    Deadline(LocalDate startDate, String reason, Period recurrence, LocalDate endDate) {
        validateDates(startDate, endDate);
        
        this.id = UUID.randomUUID();
        this.startDate = startDate;
        this.reason = reason;
        this.recurrence = recurrence;
        this.endDate = endDate;
    }

    // getters
    public UUID getId() {
        return id;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public String getReason() {
        return reason;
    }

    public Period getRecurrence() {
        return recurrence;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    // setters
    public void setStartDate(LocalDate startDate) {
        validateDates(startDate, this.endDate);
        this.startDate = startDate;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setReoccurence(Period recurrence) {
        this.recurrence = recurrence;
    }

    public void setEndDate(LocalDate endDate) {
        validateDates(this.startDate, endDate);
        this.endDate = endDate;
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "La data di fine deve essere strettamente successiva alla data di inizio.");
        }
    }

    public LocalDate getNextDate() {
        LocalDate today = LocalDate.now();

        LocalDate nextDate = this.startDate;

        if (this.recurrence == null) {
            if (nextDate.isBefore(today)) {
                return null;
            }
            return nextDate;
        }

        while (nextDate.isBefore(today)) {
            nextDate = nextDate.plus(recurrence);
        }

        if (endDate != null && nextDate.isAfter(endDate)) {
            return null;
        }

        return nextDate;
    }
}
