package com.autosalone.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.autosalone.models.catalog.AppliedItem;
import com.autosalone.models.catalog.PurchasableItem;
import com.autosalone.models.discounts.DiscountStrategy;
import com.autosalone.models.discounts.NoDiscountStrategy;

public abstract class SalesDocument {
    private UUID id;
    private LocalDate date;
    private Vehicle vehicle;
    private Customer customer;
    private List<AppliedItem> items = new ArrayList<>();
    private BigDecimal additionalFees;
    private DiscountStrategy discountStrategy;
    private boolean isVisibleToCustomer = false;
    private boolean isArchived = false;
    private String publicNotes;
    private String internalNotes;

    private BigDecimal vehicleSellingPriceSnapshot;

    protected SalesDocument(Vehicle vehicle, Customer customer) {
        this.id = UUID.randomUUID();
        this.date = LocalDate.now();
        this.vehicle = vehicle;
        this.customer = customer;
        this.items = new ArrayList<>();
        this.additionalFees = BigDecimal.ZERO;
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
        this.additionalFees = original.additionalFees;
        this.discountStrategy = original.discountStrategy;
        this.isVisibleToCustomer = false;
        this.isArchived = false;
        this.publicNotes = original.publicNotes;
        this.internalNotes = original.internalNotes;
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

    public List<AppliedItem> getItems() {
        return items;
    }

    public BigDecimal getAdditionalFees() {
        return additionalFees;
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

    public String getPublicNotes() {
        return publicNotes;
    }

    public String getInternalNotes() {
        return internalNotes;
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
        this.items.add(new AppliedItem(item));
    }

    public void setAdditionalFees(BigDecimal additionalFees) {
        validateIsEditable();
        this.additionalFees = (additionalFees == null) ? BigDecimal.ZERO : additionalFees;
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

    public void setPublicNotes(String publicNotes) {
        validateIsEditable();
        this.publicNotes = publicNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }

    public void setVehicleSellingPriceSnapshot(BigDecimal vehicleSellingPriceSnapshot) {
        validateIsEditable();
        this.vehicleSellingPriceSnapshot = vehicleSellingPriceSnapshot;
    }

    public BigDecimal getSubtotal() {
        BigDecimal total = this.vehicle.getSellingPrice();
        for (AppliedItem item : items) {
            total = total.add(item.getAppliedPrice());
        }
        return total;
    }

    public BigDecimal getFinalPrice() {
        BigDecimal subtotal = getSubtotal();
        BigDecimal discount = this.discountStrategy.calculateDiscountAmount(subtotal);
        BigDecimal discountedTotal = subtotal.subtract(discount);
        return discountedTotal.add(this.additionalFees);
    }

    protected abstract void validateIsEditable();

    protected abstract void validateIsArchivable();

    protected abstract void validateVisibilityChange(boolean isVisibleToCustomer);

}
