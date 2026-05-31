package com.autosalone.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import com.autosalone.utils.PeriodStringConverter;

@Entity
@Table(name = "deadlines")
public class Deadline {

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

    Deadline(String reason, LocalDate dueDate, Period recurrence, boolean recalculateFromCompletion,
            Vehicle vehicle) {
        java.util.Objects.requireNonNull(vehicle, "A deadline must be linked to a vehicle");
        java.util.Objects.requireNonNull(reason, "Reason is required");
        java.util.Objects.requireNonNull(dueDate, "Due date is required");

        if (recurrence == null && recalculateFromCompletion) {
            throw new IllegalArgumentException("Cannot recalculate from completion if there is no recurrence");
        }

        this.reason = reason;
        this.dueDate = dueDate;
        this.recurrence = recurrence;
        this.recalculateFromCompletion = recalculateFromCompletion;
        this.vehicle = vehicle;
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

    public boolean isExpired() {
        return !isCompleted && dueDate.isBefore(LocalDate.now());
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
        if (this.isCompleted)
            throw new IllegalStateException("Cannot edit a completed deadline");
        this.reason = reason;
    }

    public void setDueDate(LocalDate dueDate) {
        if (this.isCompleted)
            throw new IllegalStateException("Cannot edit a completed deadline");
        this.dueDate = dueDate;
    }

    public void setRecurence(Period recurrence) {
        if (this.isCompleted)
            throw new IllegalStateException("Cannot edit a completed deadline");
        this.recurrence = recurrence;
    }

    public void setRecalculateFromCompletion(boolean recalculateFromCompletion) {
        if (this.isCompleted)
            throw new IllegalStateException("Cannot edit a completed deadline");
        this.recalculateFromCompletion = recalculateFromCompletion;
    }

    /**
     * Segna la scadenza come completata e restituisce la PROSSIMA scadenza
     * generata. Restituisce null se la scadenza non è ricorrente.
     */
    public Deadline complete(LocalDate actualCompletionDate, String notes) {
        if (this.isCompleted) {
            throw new IllegalStateException("This deadline is already completed.");
        }

        this.isCompleted = true;
        this.completionDate = actualCompletionDate;
        this.notes = notes;

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
        return new Deadline(this.reason, nextDueDate, this.recurrence, this.recalculateFromCompletion, this.vehicle);
    }
}
