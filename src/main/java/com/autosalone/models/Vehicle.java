package com.autosalone.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;

public class Vehicle {
    private final UUID id;
    private String brand;
    private String model;
    private String color;
    private VehicleCondition condition;
    private Transaction purchaseTransaction;
    private BigDecimal sellingPrice;
    private LocalDate handoverDate; // data di consegna

    private List<Transaction> expenses;
    private List<Deadline> deadlines;

    private String licencePlate; // targa
    private LocalDate registrationDate; // data di immatricolazione
    private Year year;
    private double kilometers;

    private boolean isInShowroom;
    private VehicleStatus status;

    private static final String VEHICLE_INSPECTION_DEADLINE_REASON = "Revisione Veicolo";

    private Vehicle(VehicleBuilder builder) {
        this.id = UUID.randomUUID();
        this.brand = builder.brand;
        this.model = builder.model;
        this.color = builder.color;
        this.condition = builder.condition;
        this.purchaseTransaction = builder.purchaseTransaction;
        this.sellingPrice = builder.sellingPrice;
        this.handoverDate = builder.handoverDate;

        this.expenses = builder.expenses;
        this.deadlines = builder.deadlines;

        this.licencePlate = builder.licencePlate;
        this.registrationDate = builder.registrationDate;
        this.year = builder.year;
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

    public String getLicencePlate() {
        return licencePlate;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public Year getYear() {
        return year;
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
        this.brand = brand;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setCondition(VehicleCondition condition) {
        this.condition = condition;
    }

    public void setPurchaseTransaction(BigDecimal amount, LocalDate date) {
        this.purchaseTransaction = TransactionFactory.createVehiclePurchase(this.brand, this.model, amount, date);

    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public void setHandoverDate(LocalDate handoverDate) {
        this.handoverDate = handoverDate;
    }

    public void setLicencePlate(String licencePlate) {
        this.licencePlate = licencePlate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public void setYear(Year year) {
        this.year = year;
    }

    public void setKilometers(double kilometers) {
        this.kilometers = kilometers;
    }

    public void addExpense(Transaction expense) {
        this.expenses.add(expense);
    }

    public void addDeadline(LocalDate startDate, String reason, Period recurrence, LocalDate enDate) {
        Deadline deadline = new Deadline(startDate, reason, recurrence, enDate);
        this.deadlines.add(deadline);
    }

    public void setInShowroom(boolean isInShowroom) {
        this.isInShowroom = isInShowroom;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    /**
     * Genera scadenze per revisione standard: prima revisione dopo 4 anni e ogni 2
     * anni le revisioni successive (entro l'ultimo giorno del mese)
     */
    public void generateStandardInspectionDeadline() {
        if (this.registrationDate == null) {
            throw new IllegalStateException(
                    "Impossibile calcolare le scadenze di revisione standard senza una data di immatricolazione.");
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

        private List<Transaction> expenses = new ArrayList<>();
        private List<Deadline> deadlines = new ArrayList<>();

        private String licencePlate;
        private LocalDate registrationDate;
        private Year year;
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

        public VehicleBuilder setPurchaseTransaction(BigDecimal amount, LocalDate date) {
            this.purchaseTransaction = TransactionFactory.createVehiclePurchase(this.brand, this.model, amount, date);
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

        public VehicleBuilder setLicencePlate(String licencePlate) {
            this.licencePlate = licencePlate;
            return this;
        }

        public VehicleBuilder setRegistrationDate(LocalDate registrationDate) {
            this.registrationDate = registrationDate;
            return this;

        }

        public VehicleBuilder setYear(Year year) {
            this.year = year;
            return this;
        }

        public VehicleBuilder setKilometers(double kilometers) {
            this.kilometers = kilometers;
            return this;
        }

        public VehicleBuilder addExpense(String reason, BigDecimal amount, LocalDate date) {
            Transaction expense = TransactionFactory.createVehicleExpense(reason, amount, date);
            this.expenses.add(expense);
            return this;
        }

        public VehicleBuilder addDeadline(LocalDate startDate, String reason, Period recurrence, LocalDate enDate) {
            Deadline deadline = new Deadline(startDate, reason, recurrence, enDate);
            this.deadlines.add(deadline);
            return this;
        }

        public VehicleBuilder setInShowroom(boolean isInShowroom) {
            this.isInShowroom = isInShowroom;
            return this;
        }

        public Vehicle build() {
            return new Vehicle(this);
        }
    }
}
