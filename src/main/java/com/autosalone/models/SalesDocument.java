package com.autosalone.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.catalog.AppliedItem;
import com.autosalone.models.catalog.PurchasableItem;
import com.autosalone.models.discounts.DiscountStrategy;
import com.autosalone.models.discounts.FixedAmountDiscountStrategy;
import com.autosalone.models.discounts.NoDiscountStrategy;
import com.autosalone.models.discounts.PercentageDiscountStrategy;

@Entity
@Table(name = "sales_documents")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class SalesDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ElementCollection
    @CollectionTable(name = "document_items", joinColumns = @JoinColumn(name = "sales_document_id"))
    private List<AppliedItem> items = new ArrayList<>();

    @Column(name = "additional_fees")
    private BigDecimal additionalFees;

    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

    @Column(name = "public_notes", columnDefinition = "TEXT")
    private String publicNotes;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "vehicle_price_snapshot")
    private BigDecimal vehicleSellingPriceSnapshot;

    @Transient
    private DiscountStrategy discountStrategy;

    @Column(name = "discount_type", nullable = false)
    private String dbDiscountType = "NONE";

    @Column(name = "discount_value")
    private BigDecimal dbDiscountValue;

    @PrePersist
    @PreUpdate
    private void serializeDiscountStrategy() { // from Java to Database
        if (this.discountStrategy instanceof FixedAmountDiscountStrategy) {
            this.dbDiscountType = "FIXED";
            this.dbDiscountValue = ((FixedAmountDiscountStrategy) this.discountStrategy).getDiscountAmount();
        } else if (this.discountStrategy instanceof PercentageDiscountStrategy) {
            this.dbDiscountType = "PERCENTAGE";
            this.dbDiscountValue = ((PercentageDiscountStrategy) this.discountStrategy).getPercentageValue();
        } else {
            this.dbDiscountType = "NONE";
            this.dbDiscountValue = null;
        }
    }

    @PostLoad
    private void deserializeDiscountStrategy() { // from Database to Java
        switch (this.dbDiscountType) {
            case "FIXED":
                this.discountStrategy = new FixedAmountDiscountStrategy(this.dbDiscountValue);
                break;
            case "PERCENTAGE":
                this.discountStrategy = new PercentageDiscountStrategy(this.dbDiscountValue);
                break;
            default:
                this.discountStrategy = new NoDiscountStrategy();
                break;
        }
    }

    protected SalesDocument() {
    };

    protected SalesDocument(Vehicle vehicle, Customer customer) {
        VehicleStatus vs = vehicle.getStatus();
        if (vs == VehicleStatus.RESERVED || vs == VehicleStatus.SOLD || vs == VehicleStatus.WITHDRAWN) {
            throw new IllegalStateException(
                    "Cannot create sales document: the vehicle is unavailable (Status: " + vs + ")");
        }

        this.date = LocalDate.now(); // current date
        this.vehicle = vehicle;
        this.customer = customer;
        this.additionalFees = BigDecimal.ZERO;
        this.discountStrategy = new NoDiscountStrategy();
        this.isArchived = false;
        this.vehicleSellingPriceSnapshot = vehicle.getSellingPrice();
    }

    // copy constructor
    protected SalesDocument(SalesDocument original) {
        VehicleStatus vs = original.getVehicle().getStatus();
        if (vs == VehicleStatus.RESERVED || vs == VehicleStatus.SOLD || vs == VehicleStatus.WITHDRAWN) {
            throw new IllegalStateException(
                    "Cannot create sales document: the vehicle is unavailable (Status: " + vs + ")");
        }

        this.date = LocalDate.now(); // current date
        this.vehicle = original.getVehicle();
        this.customer = original.getCustomer();
        this.additionalFees = original.additionalFees;
        this.discountStrategy = original.discountStrategy;
        this.isArchived = false;
        this.publicNotes = original.publicNotes;
        this.internalNotes = original.internalNotes;
        this.vehicleSellingPriceSnapshot = original.vehicleSellingPriceSnapshot;

        this.items = new ArrayList<>(); // items deep copy
        for (AppliedItem originalItem : original.getItems()) {
            AppliedItem newItem = new AppliedItem(originalItem.getItem());
            newItem.setAppliedPrice(originalItem.getAppliedPrice());
            this.items.add(newItem);
        }
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

    public boolean isVisibleToCustomer() {
        return !isDraft();
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

    public DiscountStrategy getDiscountStrategy() {
        return discountStrategy;
    }

    public BigDecimal getDiscountAmount() {
        BigDecimal subtotal = getSubtotal();
        return this.discountStrategy.calculateDiscountAmount(subtotal);
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

    public void setAdditionalFees(BigDecimal additionalFees) {
        validateIsEditable();
        this.additionalFees = (additionalFees == null) ? BigDecimal.ZERO : additionalFees;
    }

    public void archive() {
        if (this.isArchived)
            return;
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

    public void setDiscountStrategy(DiscountStrategy discountStrategy) {
        validateIsEditable();
        this.discountStrategy = discountStrategy;
    }

    // items
    public void setAppliedItemPrice(PurchasableItem item, BigDecimal newPrice) {
        validateIsEditable();
        AppliedItem targetItem = findAppliedItem(item);
        targetItem.setAppliedPrice(newPrice);
    }

    public void addItem(PurchasableItem item) {
        validateIsEditable();

        java.util.Objects.requireNonNull(item, "Item is required");
        item.validateIsNotArchived();
        validatePurchasableItemAlreadyExists(item);

        this.items.add(new AppliedItem(item));
    }

    public void removeItem(PurchasableItem item) {
        validateIsEditable();

        java.util.Objects.requireNonNull(item, "Item is required");

        AppliedItem targetItem = findAppliedItem(item);
        this.items.remove(targetItem);
    }

    private AppliedItem findAppliedItem(PurchasableItem item) {
        return this.items.stream()
                .filter(applied -> applied.getItem().getName().equalsIgnoreCase(item.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found in this document"));
    }

    private void validatePurchasableItemAlreadyExists(PurchasableItem item) {
        boolean alreadyExists = this.items.stream()
                .anyMatch(applied -> applied.getItem().getName().equalsIgnoreCase(item.getName()));
        if (alreadyExists)
            throw new IllegalArgumentException("This item is already in the document");
    }

    // abstract methods
    protected abstract void validateIsEditable();

    protected abstract void validateIsArchivable();

    protected abstract boolean isDraft();
}
