package com.autosalone.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;

@Entity
@Table(name = "vehicles")
public class Vehicle {

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

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    private List<Deadline> deadlines = new ArrayList<>();

    @Column(name = "license_plate")
    private String licensePlate; // targa

    @Column(name = "registration_date")
    private LocalDate registrationDate; // data di immatricolazione

    @Column
    private double kilometers;

    @Column(name = "is_in_showroom", nullable = false)
    private boolean isInShowroom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    private static final String VEHICLE_INSPECTION_DEADLINE_REASON = "Revisione Veicolo";

    protected Vehicle() {
    }

    private Vehicle(VehicleBuilder builder) {
        this.brand = builder.brand;
        this.model = builder.model;
        this.color = builder.color;
        this.condition = builder.condition;
        this.purchaseTransaction = builder.purchaseTransaction;
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

    public double getKilometers() {
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

    public void setPurchaseTransaction(Transaction purchaseTransaction) {
        assertNotTerminal();
        this.purchaseTransaction = purchaseTransaction;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        assertNotTerminal();
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

    public void setKilometers(double kilometers) {
        assertCoreEditable();
        this.kilometers = kilometers;
    }

    public void addExpense(Transaction expense) {
        assertNotTerminal();
        this.expenses.add(expense);
    }

    public void addDeadline(LocalDate startDate, String reason, Period recurrence, LocalDate enDate) {
        if (this.status == VehicleStatus.WITHDRAWN) {
            throw new IllegalStateException("Cannot edit an inactive vehicle (Status: " + this.status + ")");
        }
        Deadline deadline = new Deadline(startDate, reason, recurrence, enDate, this);
        this.deadlines.add(deadline);
    }

    public void setInShowroom(boolean isInShowroom) {
        assertNotTerminal();
        this.isInShowroom = isInShowroom;
    }

    public void setStatus(VehicleStatus status) {
        if (this.status == status)
            return;
        if (this.status == VehicleStatus.SOLD || this.status == VehicleStatus.WITHDRAWN) {
            throw new IllegalStateException("Cannot transition out of a terminal state (" + this.status + ")");
        }
        if (status == VehicleStatus.SOLD || status == VehicleStatus.WITHDRAWN) {
            this.isInShowroom = false;
        }
        this.status = status;
    }

    // validation helper methods
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
     * Genera scadenze per revisione standard: prima revisione dopo 4 anni e ogni 2
     * anni le revisioni successive (entro l'ultimo giorno del mese)
     */
    public void generateStandardInspectionDeadline() {
        if (this.registrationDate == null) {
            throw new IllegalStateException(
                    "Cannot generate standard inspection deadlines without a registration date");
        }

        LocalDate firstInspectionDate = this.registrationDate
                .plusYears(4)
                .with(TemporalAdjusters.lastDayOfMonth());

        Period standardRecurrence = Period.ofYears(2);

        this.addDeadline(firstInspectionDate, VEHICLE_INSPECTION_DEADLINE_REASON, standardRecurrence, null);
    }

    // builder
    public static class VehicleBuilder {
        private String brand;
        private String model;
        private String color;
        private VehicleCondition condition;
        private Transaction purchaseTransaction;
        private BigDecimal sellingPrice;
        private LocalDate handoverDate;
        private String licensePlate;
        private LocalDate registrationDate;
        private double kilometers;
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

        public VehicleBuilder setPurchaseTransaction(Transaction purchaseTransaction) {
            this.purchaseTransaction = purchaseTransaction;
            return this;
        }

        public VehicleBuilder setSellingPrice(BigDecimal sellingPrice) {
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

        public VehicleBuilder setKilometers(double kilometers) {
            this.kilometers = kilometers;
            return this;
        }

        public VehicleBuilder setInShowroom(boolean isInShowroom) {
            this.isInShowroom = isInShowroom;
            return this;
        }

        public Vehicle build() {
            java.util.Objects.requireNonNull(this.brand, "Brand is required");
            java.util.Objects.requireNonNull(this.model, "Model is required");
            java.util.Objects.requireNonNull(this.color, "Color is required");
            java.util.Objects.requireNonNull(this.condition, "Condition is required");
            java.util.Objects.requireNonNull(this.isInShowroom, "Is in showroom is required");
            return new Vehicle(this);
        }
    }
}
