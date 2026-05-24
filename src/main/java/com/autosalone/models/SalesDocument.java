package com.autosalone.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(name = "is_visible_to_customer", nullable = false)
    private boolean isVisibleToCustomer = false;

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

    protected SalesDocument(Vehicle vehicle, Customer customer) {
        this.date = LocalDate.now();
        this.vehicle = vehicle;
        this.customer = customer;
        this.additionalFees = BigDecimal.ZERO;
        this.discountStrategy = new NoDiscountStrategy();
        this.isVisibleToCustomer = false;
        this.isArchived = false;
        this.vehicleSellingPriceSnapshot = vehicle.getSellingPrice();
    }

    // copy constructor
    protected SalesDocument(SalesDocument original) {
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

    public DiscountStrategy getDiscountStrategy() {
        return discountStrategy;
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

    public void setDiscountStrategy(DiscountStrategy discountStrategy) {
        validateIsEditable();
        this.discountStrategy = discountStrategy;
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
