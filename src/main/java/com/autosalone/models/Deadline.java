package com.autosalone.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

import com.autosalone.utils.PeriodStringConverter;

@Entity
@Table(name = "deadlines")
public class Deadline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "recurrence")
    @Convert(converter = PeriodStringConverter.class)
    private Period recurrence;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_vehicle_id")
    private Vehicle vehicle;

    protected Deadline() {
    }

    Deadline(LocalDate startDate, String reason, Period recurrence, LocalDate endDate) {
        validateDates(startDate, endDate);

        this.startDate = startDate;
        this.reason = reason;
        this.recurrence = recurrence;
        this.endDate = endDate;
    }

    Deadline(LocalDate startDate, String reason, Period recurrence, LocalDate endDate, Vehicle vehicle) {
        this(startDate, reason, recurrence, endDate);
        this.vehicle = vehicle;
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
            throw new IllegalArgumentException("The end date must be stricly after the start date");
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
