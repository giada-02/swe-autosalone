package com.autosalone.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;

@Entity
@Table(name = "vehicles")
public class Vehicle extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleCondition condition;

    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "purchase_transaction_id", referencedColumnName = "id", unique = true)
    private Transaction purchaseTransaction;

    @Column(name = "selling_price")
    private BigDecimal sellingPrice;

    @Column(name = "handover_date")
    private LocalDate handoverDate; // data di consegna effettiva

    @OneToMany(mappedBy = "vehicle", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<Transaction> expenses = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Deadline> deadlines = new ArrayList<>();

    @Column(name = "license_plate")
    private String licensePlate; // targa

    @Column(name = "registration_date")
    private LocalDate registrationDate; // data di immatricolazione

    @Column
    private Double kilometers;

    @Column(name = "is_in_showroom", nullable = false)
    private boolean isInShowroom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column(name = "withdrawal_reason", columnDefinition = "TEXT")
    private String withdrawalReason;

    protected Vehicle() {
    }

    private Vehicle(VehicleBuilder builder) {
        this.brand = builder.brand;
        this.model = builder.model;
        this.color = builder.color;
        this.condition = builder.condition;
        this.sellingPrice = builder.sellingPrice;
        this.handoverDate = builder.handoverDate;
        this.licensePlate = builder.licensePlate;
        this.registrationDate = builder.registrationDate;
        this.kilometers = builder.kilometers;
        this.isInShowroom = builder.isInShowroom;
        this.status = VehicleStatus.AVAILABLE;
    }

    // getters
    public UUID getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getColor() {
        return color;
    }

    public String getModel() {
        return model;
    }

    public VehicleCondition getCondition() {
        return condition;
    }

    public Transaction getPurchaseTransaction() {
        return purchaseTransaction;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public LocalDate getHandoverDate() {
        return handoverDate;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public Double getKilometers() {
        return kilometers;
    }

    public List<Transaction> getExpenses() {
        return expenses;
    }

    public List<Deadline> getDeadlines() {
        return deadlines;
    }

    public boolean isInShowroom() {
        return isInShowroom;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public String getWithdrawalReason() {
        return withdrawalReason;
    }

    // setters
    public void setBrand(String brand) {
        assertCoreEditable();
        this.brand = brand;
    }

    public void setModel(String model) {
        assertCoreEditable();
        this.model = model;
    }

    public void setColor(String color) {
        assertCoreEditable();
        this.color = color;
    }

    public void setCondition(VehicleCondition condition) {
        assertCoreEditable();
        this.condition = condition;
    }

    public void setPurchaseTransaction(Transaction purchaseTransaction) {
        assertNotWithdrawn();
        if (this.purchaseTransaction != null) {
            throw new IllegalStateException(
                    "The purchase transaction has already been registered and cannot be overwritten");
        }
        this.purchaseTransaction = purchaseTransaction;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        assertNotTerminal();
        if (sellingPrice != null && sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Selling price cannot be negative");
        }
        this.sellingPrice = sellingPrice;
    }

    public void setHandoverDate(LocalDate handoverDate) {
        assertNotTerminal();
        this.handoverDate = handoverDate;
    }

    public void setLicensePlate(String licensePlate) {
        assertAnagraphicEditable();
        this.licensePlate = licensePlate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        assertAnagraphicEditable();
        this.registrationDate = registrationDate;
    }

    public void setKilometers(Double kilometers) {
        assertCoreEditable();
        if (kilometers != null && kilometers < 0) {
            throw new IllegalArgumentException("Kilometers cannot be negative");
        }
        this.kilometers = kilometers;
    }

    public void addExpense(Transaction expense) {
        assertNotTerminal();
        this.expenses.add(expense);
    }

    public void addDeadline(String reason, LocalDate dueDate, Period recurrence, boolean recalculateFromCompletion) {
        assertNotWithdrawn();
        Deadline deadline = new Deadline(this, reason, dueDate, recurrence, recalculateFromCompletion);
        this.deadlines.add(deadline);
    }

    public void removeDeadline(Deadline deadline) {
        this.deadlines.remove(deadline);
    }

    public void setIsInShowroom(boolean isInShowroom) {
        assertNotTerminal();
        this.isInShowroom = isInShowroom;
    }

    public void setStatus(VehicleStatus status) {
        if (this.status == status)
            return;

        if (this.status == VehicleStatus.SOLD || this.status == VehicleStatus.WITHDRAWN) {
            throw new IllegalStateException("Cannot transition out of a terminal state (" + this.status + ")");
        }

        if (status == VehicleStatus.WITHDRAWN)
            throw new IllegalArgumentException("Use the withdraw(reason) method to transition to WITHDRAWN");

        if (status == VehicleStatus.SOLD)
            this.isInShowroom = false;

        this.status = status;
    }

    public void withdraw(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A withdrawal reason must be provided");
        }

        if (this.status == VehicleStatus.SOLD || this.status == VehicleStatus.WITHDRAWN) {
            throw new IllegalStateException("Cannot transition out of a terminal state (" + this.status + ")");
        }

        this.status = VehicleStatus.WITHDRAWN;
        this.withdrawalReason = reason;
        this.isInShowroom = false;
    }

    // validation helper methods
    private void assertNotWithdrawn() {
        if (this.status == VehicleStatus.WITHDRAWN) {
            throw new IllegalStateException("Cannot edit an inactive vehicle (Status: " + this.status + ")");
        }
    }

    private void assertNotTerminal() {
        if (this.status == VehicleStatus.SOLD || this.status == VehicleStatus.WITHDRAWN) {
            throw new IllegalStateException("Cannot edit an inactive vehicle (Status: " + this.status + ")");
        }
    }

    private void assertNotInNegotiation() {
        if (this.status == VehicleStatus.QUOTED || this.status == VehicleStatus.RESERVED) {
            throw new IllegalStateException(
                    "Cannot edit core vehicle data: it is currently locked in a commercial negotiation (Status: "
                            + this.status + ")");
        }
    }

    private void assertCoreEditable() {
        assertNotTerminal();
        assertNotInNegotiation();
    }

    private void assertAnagraphicEditable() {
        assertNotTerminal();
        if (this.condition == VehicleCondition.SECONDHAND && this.status != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Registration data for second-hand vehicles is locked during commercial negotiations (Status "
                            + this.status + ")");
        }
    }

    /**
     * Genera la prima scadenza per revisione standard: prima revisione dopo 4
     * anni e ogni 2 anni le revisioni successive (entro l'ultimo giorno del mese)
     */
    public void generateStandardInspectionDeadline() {
        if (this.registrationDate == null) {
            throw new IllegalStateException(
                    "Cannot generate the standard inspection deadline without a registration date");
        }

        LocalDate firstInspectionDate = this.registrationDate
                .plusYears(4)
                .with(TemporalAdjusters.lastDayOfMonth());

        if (firstInspectionDate.isBefore(LocalDate.now())) {
            throw new IllegalStateException(
                    "Cannot generate standard inspection deadline: the vehicle registration date is more than 4 years old");
        }

        Period standardRecurrence = Period.ofYears(2);

        this.addDeadline(Deadline.VEHICLE_INSPECTION_REASON, firstInspectionDate, standardRecurrence, true);
    }

    /**
     * Genera la prossima scadenza per la revisione basandosi sulla data dell'ultima
     * revisione effettivamente eseguita e annotata sul libretto di circolazione.
     * * @param lastInspection La data esatta in cui è stata superata l'ultima
     * revisione.
     */
    public void generateInspectionFromLastDate(LocalDate lastInspection) {
        Objects.requireNonNull(lastInspection, "The last actual inspection date is required");

        LocalDate nextInspectionDate = lastInspection
                .plusYears(2)
                .with(TemporalAdjusters.lastDayOfMonth());

        Period standardRecurrence = Period.ofYears(2);

        this.addDeadline(Deadline.VEHICLE_INSPECTION_REASON, nextInspectionDate, standardRecurrence, true);
    }

    // builder
    public static class VehicleBuilder {
        private String brand;
        private String model;
        private String color;
        private VehicleCondition condition;
        private BigDecimal sellingPrice;
        private LocalDate handoverDate;
        private String licensePlate;
        private LocalDate registrationDate;
        private Double kilometers;
        private boolean isInShowroom;

        public VehicleBuilder setBrand(String brand) {
            this.brand = brand;
            return this;
        }

        public VehicleBuilder setModel(String model) {
            this.model = model;
            return this;
        }

        public VehicleBuilder setColor(String color) {
            this.color = color;
            return this;
        }

        public VehicleBuilder setCondition(VehicleCondition condition) {
            this.condition = condition;
            return this;
        }

        public VehicleBuilder setSellingPrice(BigDecimal sellingPrice) {
            if (sellingPrice != null && sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Selling price cannot be negative");
            }
            this.sellingPrice = sellingPrice;
            return this;
        }

        public VehicleBuilder setHandoverDate(LocalDate handoverDate) {
            this.handoverDate = handoverDate;
            return this;
        }

        public VehicleBuilder setLicensePlate(String licensePlate) {
            this.licensePlate = licensePlate;
            return this;
        }

        public VehicleBuilder setRegistrationDate(LocalDate registrationDate) {
            this.registrationDate = registrationDate;
            return this;
        }

        public VehicleBuilder setKilometers(Double kilometers) {
            if (kilometers != null && kilometers < 0) {
                throw new IllegalArgumentException("Kilometers cannot be negative");
            }
            this.kilometers = kilometers;
            return this;
        }

        public VehicleBuilder setIsInShowroom(boolean isInShowroom) {
            this.isInShowroom = isInShowroom;
            return this;
        }

        public Vehicle build() {
            Objects.requireNonNull(this.brand, "Brand is required");
            Objects.requireNonNull(this.model, "Model is required");
            Objects.requireNonNull(this.color, "Color is required");
            Objects.requireNonNull(this.condition, "Condition is required");
            Objects.requireNonNull(this.isInShowroom, "Is in showroom is required");
            return new Vehicle(this);
        }
    }

    public void validateVehicleStatusForDocument(String errorTitle) {
        if (this.status == VehicleStatus.RESERVED || this.status == VehicleStatus.SOLD
                || this.status == VehicleStatus.WITHDRAWN) {
            throw new IllegalStateException(
                    errorTitle + ": the vehicle is unavailable (Status: " + this.status + ")");
        }
    }
}
