package com.autosalone.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;
import java.util.UUID;

import com.autosalone.utils.PeriodStringConverter;

@Entity
@Table(name = "deadlines")
public class Deadline extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // dati dell'evento

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_vehicle_id")
    private Vehicle vehicle;

    // regole di ripetizione

    @Column(name = "recurrence")
    @Convert(converter = PeriodStringConverter.class)
    private Period recurrence;

    @Column(name = "recalculate_from_completion", nullable = false)
    private boolean recalculateFromCompletion = false;

    // dati di completamento

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public static final String VEHICLE_INSPECTION_REASON = "Revisione Veicolo";

    protected Deadline() {
    }

    Deadline(Vehicle vehicle, String reason, LocalDate dueDate, Period recurrence, boolean recalculateFromCompletion) {
        validateRecalculateCompletionWithoutRecurrence(recalculateFromCompletion, recurrence);
        this.reason = requireReason(reason);
        this.dueDate = requireDate(dueDate, "Due date is required");
        this.recurrence = recurrence;
        this.recalculateFromCompletion = recalculateFromCompletion;
        this.vehicle = requireVehicle(vehicle);
    }

    // getters
    public UUID getId() {
        return id;
    }

    public String getReason() {
        return reason;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public boolean isExpired(LocalDate today) {
        return !isCompleted && dueDate.isBefore(today);
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Period getRecurrence() {
        return recurrence;
    }

    public boolean isRecalculatedFromCompletion() {
        return recalculateFromCompletion;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public String getNotes() {
        return notes;
    }

    // setters
    public void setReason(String reason) {
        String normalizedReason = requireReason(reason);
        if (Objects.equals(this.reason, normalizedReason))
            return;
        validateNotCompleted();
        this.reason = normalizedReason;
    }

    public void setDueDate(LocalDate dueDate) {
        requireDate(dueDate, "Due date is required");
        if (Objects.equals(this.dueDate, dueDate))
            return;
        validateNotCompleted();
        this.dueDate = dueDate;
    }

    public void setRecurrence(Period recurrence) {
        if (Objects.equals(this.recurrence, recurrence))
            return;
        validateNotCompleted();
        if (recurrence == null && this.recalculateFromCompletion)
            this.recalculateFromCompletion = false;
        this.recurrence = recurrence;
    }

    public void setRecalculateFromCompletion(boolean recalculateFromCompletion) {
        if (this.recalculateFromCompletion == recalculateFromCompletion) {
            return;
        }
        validateNotCompleted();
        validateRecalculateCompletionWithoutRecurrence(recalculateFromCompletion, recurrence);
        this.recalculateFromCompletion = recalculateFromCompletion;
    }

    /**
     * Segna la scadenza come completata e restituisce la PROSSIMA scadenza
     * generata. Restituisce null se la scadenza non è ricorrente.
     */
    public Deadline complete(LocalDate actualCompletionDate, String notes) {
        if (this.isCompleted)
            throw new IllegalStateException("This deadline is already completed");
        requireDate(actualCompletionDate, "Actual completion date is required");

        this.isCompleted = true;
        this.completionDate = actualCompletionDate;
        this.notes = normalizeNotes(notes);

        if (this.recurrence == null) {
            return null; // scadenza singola, non si ripete
        }

        LocalDate nextDueDate;
        if (this.recalculateFromCompletion) {
            nextDueDate = actualCompletionDate.plus(this.recurrence);

            // regola speciale per la revisione: fine del mese
            if (Deadline.VEHICLE_INSPECTION_REASON.equals(this.reason)) {
                nextDueDate = nextDueDate.with(TemporalAdjusters.lastDayOfMonth());
            }
        } else {
            nextDueDate = this.dueDate.plus(this.recurrence);
        }

        // crea il nuovo evento per il futuro
        return new Deadline(this.vehicle, this.reason, nextDueDate, this.recurrence, this.recalculateFromCompletion);
    }

    // validation and normalization
    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("Reason is required");
        return reason.trim();
    }

    private static LocalDate requireDate(LocalDate date, String message) {
        if (date == null)
            throw new IllegalArgumentException(message);
        return date;
    }

    private static Vehicle requireVehicle(Vehicle vehicle) {
        if (vehicle == null)
            throw new IllegalArgumentException("A deadline must be linked to a vehicle");
        return vehicle;
    }

    private static String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank())
            return null;
        return notes.trim();
    }

    private static void validateRecalculateCompletionWithoutRecurrence(boolean recalculateFromCompletion,
            Period recurrence) {
        if (recalculateFromCompletion && recurrence == null)
            throw new IllegalArgumentException("Cannot recalculate from completion if there is no recurrence");
    }

    // state validation
    private void validateNotCompleted() {
        if (this.isCompleted)
            throw new IllegalStateException("Cannot edit a completed deadline");
    }

}
