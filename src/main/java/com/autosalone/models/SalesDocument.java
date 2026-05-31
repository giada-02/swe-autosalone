package com.autosalone.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
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
    @MapKeyColumn(name = "item_name")
    private Map<String, AppliedItem> items = new HashMap<>();

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

        this.items = new HashMap<>(); // items deep copy
        for (Map.Entry<String, AppliedItem> entry : original.getItems().entrySet()) {
            AppliedItem originalAppliedItem = entry.getValue();

            AppliedItem newAppliedItem = new AppliedItem(originalAppliedItem.getItem());
            newAppliedItem.setAppliedPrice(originalAppliedItem.getAppliedPrice());

            this.items.put(entry.getKey(), newAppliedItem);
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

    public Map<String, AppliedItem> getItems() {
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
        for (AppliedItem item : items.values()) {
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

    public void addItem(PurchasableItem item) {
        validateIsEditable();
        if (item == null || item.getName() == null) {
            throw new IllegalArgumentException("PurchasableItem and its name cannot be null");
        }
        this.items.put(item.getName(), new AppliedItem(item));
    }

    public void removeItem(PurchasableItem item) {
        validateIsEditable();
        if (item == null || item.getName() == null) {
            throw new IllegalArgumentException("PurchasableItem and its name cannot be null");
        }
        findAppliedItem(item);
        this.items.remove(item.getName());
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

    public void setAppliedItemPrice(PurchasableItem item, BigDecimal newPrice) {
        validateIsEditable();
        AppliedItem targetItem = findAppliedItem(item);
        targetItem.setAppliedPrice(newPrice);
    }

    public AppliedItem findAppliedItem(PurchasableItem item) {
        if (!this.items.containsKey(item.getName()))
            throw new IllegalArgumentException("Item not found in this document");
        return this.items.get(item.getName());
    }

    protected abstract void validateIsEditable();

    protected abstract void validateIsArchivable();

    protected abstract boolean isDraft();
}
