package com.autosalone.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.autosalone.models.catalog.PurchasableItem;
import com.autosalone.models.discounts.DiscountStrategy;
import com.autosalone.models.discounts.NoDiscountStrategy;

public abstract class SalesDocument {
    private final UUID id;
    private LocalDate date;
    private Vehicle vehicle;
    private Customer customer;
    private List<PurchasableItem> items;
    private BigDecimal fees;
    private DiscountStrategy discountStrategy;
    private boolean isVisibleToCustomer = false;
    private boolean isArchived = false;

    private BigDecimal vehicleSellingPriceSnapshot;

    protected SalesDocument(Vehicle vehicle, Customer customer) {
        this.id = UUID.randomUUID();
        this.date = LocalDate.now();
        this.vehicle = vehicle;
        this.customer = customer;
        this.items = new ArrayList<>();
        this.fees = BigDecimal.ZERO;
        this.discountStrategy = new NoDiscountStrategy();
        this.isVisibleToCustomer = false;
        this.isArchived = false;
        this.vehicleSellingPriceSnapshot = vehicle.getSellingPrice();
    }

    // copy constructor
    protected SalesDocument(SalesDocument original) {
        this.id = UUID.randomUUID();
        this.date = LocalDate.now(); // current date
        this.vehicle = original.getVehicle();
        this.customer = original.getCustomer();
        this.items = new ArrayList<>(original.getItems());
        this.fees = original.fees;
        this.discountStrategy = original.discountStrategy;
        this.isVisibleToCustomer = false;
        this.isArchived = false;
        this.vehicleSellingPriceSnapshot = original.vehicleSellingPriceSnapshot;
    }

    // getters
    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<PurchasableItem> getItems() {
        return items;
    }

    public BigDecimal getFees() {
        return fees;
    }

    public DiscountStrategy getDiscountStrategy() {
        return discountStrategy;
    }

    public boolean isVisibleToCustomer() {
        return isVisibleToCustomer;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public BigDecimal getVehicleSellingPriceSnapshot() {
        return vehicleSellingPriceSnapshot;
    }

    // setters
    public void setDate(LocalDate date) {
        validateIsEditable();
        this.date = date;
    }

    public void setVehicle(Vehicle vehicle) {
        validateIsEditable();
        this.vehicle = vehicle;
    }

    public void setCustomer(Customer customer) {
        validateIsEditable();
        this.customer = customer;
    }

    public void addItem(PurchasableItem item) {
        validateIsEditable();
        this.items.add(item);
    }

    public void setFees(BigDecimal fees) {
        validateIsEditable();
        this.fees = (fees == null) ? BigDecimal.ZERO : fees;
    }

    public void setDiscountStrategy(DiscountStrategy discountStrategy) {
        validateIsEditable();
        this.discountStrategy = discountStrategy;
    }

    public void setVisibleToCustomer(boolean isVisibleToCustomer) {
        if (this.isVisibleToCustomer == isVisibleToCustomer)
            return;
        validateVisibilityChange(isVisibleToCustomer);
        this.isVisibleToCustomer = isVisibleToCustomer;
    }

    public void archive() {
        if (this.isArchived)
            return;
        if (this.isVisibleToCustomer)
            throw new IllegalStateException("Cannot archive document while visible to the customer");
        validateIsArchivable();
        this.isArchived = true;
    }

    public void unarchive() {
        if (!this.isArchived)
            return;
        this.isArchived = false;
    }

    public void setVehicleSellingPriceSnapshot(BigDecimal vehicleSellingPriceSnapshot) {
        validateIsEditable();
        this.vehicleSellingPriceSnapshot = vehicleSellingPriceSnapshot;
    }

    public BigDecimal getSubtotal() {
        BigDecimal total = this.vehicle.getSellingPrice();
        for (PurchasableItem item : items) {
            total = total.add(item.getPrice());
        }
        return total;
    }

    public BigDecimal getFinalPrice() {
        BigDecimal subtotal = getSubtotal();
        BigDecimal discount = this.discountStrategy.calculateDiscountAmount(subtotal);
        BigDecimal discountedTotal = subtotal.subtract(discount);
        return discountedTotal.add(this.fees);
    }

    protected abstract void validateIsEditable();

    protected abstract void validateIsArchivable();

    protected abstract void validateVisibilityChange(boolean isVisibleToCustomer);

}
