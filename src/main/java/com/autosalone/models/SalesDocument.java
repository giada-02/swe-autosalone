package com.autosalone.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.autosalone.enums.DiscountType;
import com.autosalone.models.catalog.AppliedItem;
import com.autosalone.models.catalog.PurchasableItem;
import com.autosalone.models.catalog.visitors.ActiveItemValidatorVisitor;
import com.autosalone.models.catalog.visitors.HierarchyValidationVisitor;
import com.autosalone.models.discounts.DiscountStrategy;
import com.autosalone.models.discounts.NoDiscountStrategy;
import com.autosalone.utils.Utils;

@Entity
@Table(name = "sales_documents")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class SalesDocument extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ElementCollection(fetch = FetchType.EAGER)
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

    @Column(name = "vehicle_price_snapshot", nullable = false)
    private BigDecimal vehicleSellingPriceSnapshot;

    @Transient
    private DiscountStrategy discountStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType dbDiscountType = DiscountType.NONE;

    @Column(name = "discount_value")
    private BigDecimal dbDiscountValue;

    @PrePersist
    @PreUpdate
    protected void normalizeData() {
        this.publicNotes = Utils.sanitizeText(this.publicNotes);
        this.internalNotes = Utils.sanitizeText(this.internalNotes);
    }

    @PostLoad
    private void deserializeDiscountStrategy() {
        if (this.dbDiscountType != null) {
            this.discountStrategy = this.dbDiscountType.createStrategy(this.dbDiscountValue);
        } else {
            this.discountStrategy = new NoDiscountStrategy();
            this.dbDiscountType = DiscountType.NONE;
        }
    }

    protected SalesDocument() {
    };

    protected SalesDocument(Vehicle vehicle, Customer customer) {
        Objects.requireNonNull(vehicle, "Vehicle is required");
        Objects.requireNonNull(customer, "Customer is required");
        vehicle.validateVehicleStatusForDocument("Cannot create sales document");

        this.date = LocalDate.now(); // current date
        this.vehicle = vehicle;
        this.customer = customer;
        this.additionalFees = BigDecimal.ZERO;
        this.discountStrategy = new NoDiscountStrategy();
        this.isArchived = false;
        this.vehicleSellingPriceSnapshot = vehicle.getSellingPrice();
    }

    /// Conversion Constructor (from quotation to contract)
    // Conserva i prezzi stabiliti e gli item presenti (anche archiviati) nel
    // documento originale
    protected SalesDocument(SalesDocument original) {
        original.getVehicle().validateVehicleStatusForDocument("Cannot create sales document");

        this.date = LocalDate.now(); // current date
        this.vehicle = original.getVehicle();
        this.customer = original.getCustomer();
        this.additionalFees = original.getAdditionalFees();
        this.discountStrategy = original.getDiscountStrategy();
        this.isArchived = false;
        this.publicNotes = original.getPublicNotes();
        this.internalNotes = original.getInternalNotes();
        this.vehicleSellingPriceSnapshot = original.getVehicleSellingPriceSnapshot(); // snapshot selling price

        this.items = new ArrayList<>(); // items deep copy
        for (AppliedItem originalItem : original.getItems()) {
            this.items.add(new AppliedItem(originalItem)); // snapshot applied price
        }
    }

    /// Cloning Constructor (from old quotation to new quotation)
    // Aggiorna ai prezzi di listino attuali e fa pulizia degli accessori
    // archiviati
    protected SalesDocument(SalesDocument original, boolean skipArchived) {
        original.getVehicle().validateVehicleStatusForDocument("Cannot create sales document");

        this.date = LocalDate.now();
        this.vehicle = original.getVehicle();
        this.customer = original.getCustomer();
        this.additionalFees = original.getAdditionalFees();
        this.discountStrategy = original.getDiscountStrategy();
        this.isArchived = false;
        this.publicNotes = original.getPublicNotes();
        this.internalNotes = original.getInternalNotes();
        this.vehicleSellingPriceSnapshot = original.getVehicle().getSellingPrice(); // updated vehicle selling price

        this.items = new ArrayList<>(); // items deep copy

        ActiveItemValidatorVisitor activeInspector = new ActiveItemValidatorVisitor();

        for (AppliedItem originalItem : original.getItems()) {
            PurchasableItem catalogItem = originalItem.getItem();
            if (skipArchived) {
                try {
                    catalogItem.accept(activeInspector);
                } catch (IllegalArgumentException e) {
                    continue; // skips archived items and items containing archived items
                }
            }
            this.items.add(new AppliedItem(originalItem, true)); // updated items price
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

    public DiscountType getDiscountType() {
        return dbDiscountType;
    }

    public BigDecimal getDiscountValue() {
        return dbDiscountValue;
    }

    public DiscountStrategy getDiscountStrategy() {
        return discountStrategy;
    }

    public BigDecimal getDiscountAmount() {
        BigDecimal subtotal = getSubtotal();
        return this.discountStrategy != null ? this.discountStrategy.calculateDiscountAmount(subtotal)
                : BigDecimal.ZERO;
    }

    public BigDecimal getSubtotal() {
        BigDecimal total = vehicleSellingPriceSnapshot != null ? vehicleSellingPriceSnapshot : BigDecimal.ZERO;
        for (AppliedItem item : items) {
            total = total.add(item.getAppliedPrice());
        }
        return total;
    }

    public BigDecimal getFinalPrice() {
        BigDecimal subtotal = getSubtotal();
        BigDecimal discount = getDiscountAmount();
        BigDecimal discountedTotal = subtotal.subtract(discount);
        return discountedTotal.add(this.additionalFees);
    }

    // setters
    public void setDate(LocalDate date) {
        Objects.requireNonNull(date, "Date is required");
        if (Objects.equals(this.date, date))
            return;
        validateIsEditable();
        this.date = date;
    }

    public void setVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle is required");
        if (Objects.equals(this.vehicle, vehicle))
            return;
        validateIsEditable();
        this.vehicle = vehicle;
    }

    public void setCustomer(Customer customer) {
        Objects.requireNonNull(customer, "Customer is required");
        if (Objects.equals(this.customer, customer))
            return;
        validateIsEditable();
        this.customer = customer;
    }

    public void setAdditionalFees(BigDecimal additionalFees) {
        BigDecimal fees = additionalFees != null ? additionalFees : BigDecimal.ZERO;
        if (this.additionalFees != null && this.additionalFees.compareTo(fees) == 0)
            return;

        validateIsEditable();
        if (fees.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Additional fees cannot be negative");

        this.additionalFees = fees;
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
        if (Objects.equals(this.publicNotes, publicNotes))
            return;
        validateIsEditable();
        this.publicNotes = publicNotes;
    }

    public void setInternalNotes(String internalNotes) {
        if (Objects.equals(this.internalNotes, internalNotes))
            return;
        this.internalNotes = internalNotes;
    }

    public void setVehicleSellingPriceSnapshot(BigDecimal vehicleSellingPriceSnapshot) {
        Objects.requireNonNull(vehicleSellingPriceSnapshot, "The vehicle selling price is required");
        if (this.vehicleSellingPriceSnapshot != null
                && this.vehicleSellingPriceSnapshot.compareTo(vehicleSellingPriceSnapshot) == 0)
            return;

        validateIsEditable();
        if (vehicleSellingPriceSnapshot.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("The vehicle selling price cannot negative");
        this.vehicleSellingPriceSnapshot = vehicleSellingPriceSnapshot;
    }

    public void setDiscountStrategy(DiscountStrategy discountStrategy) {
        DiscountStrategy newDiscountStrategy = discountStrategy != null ? discountStrategy : new NoDiscountStrategy();
        if (this.discountStrategy != null && this.discountStrategy.equals(newDiscountStrategy))
            return;

        validateIsEditable();

        this.discountStrategy = newDiscountStrategy;
        this.dbDiscountType = this.discountStrategy.getType();
        this.dbDiscountValue = this.discountStrategy.getValue();
    }

    // items
    public void addItem(AppliedItem appliedItem) {
        validateIsEditable();
        Objects.requireNonNull(appliedItem, "Applied item is required");

        HierarchyValidationVisitor inspector = new HierarchyValidationVisitor(null);

        try {
            for (AppliedItem existing : this.items) {
                existing.getItem().accept(inspector);
            }
            appliedItem.getItem().accept(inspector);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Cannot add item: the item or one of its components is already in the document. (" + e.getMessage()
                            + ")");
        }

        this.items.add(appliedItem);
    }

    public void removeItem(AppliedItem appliedItem) {
        validateIsEditable();
        Objects.requireNonNull(appliedItem, "Applied item is required");
        this.items.remove(appliedItem);
    }

    public void setAppliedItemPrice(AppliedItem targetItem, BigDecimal newPrice) {
        validateIsEditable();
        Objects.requireNonNull(targetItem, "Target item is required");
        targetItem.setAppliedPrice(newPrice); // validates the price
    }

    // abstract methods
    protected abstract void validateIsEditable();

    protected abstract void validateIsArchivable();

    protected abstract boolean isDraft();

    protected void validateSecondhandVehicle(String errorTitle) {
        if (this.vehicle.getRegistrationDate() == null || this.vehicle.getLicensePlate() == null
                || this.vehicle.getLicensePlate().trim().isEmpty()
                || this.vehicle.getKilometers() == null) {
            throw new IllegalStateException(
                    errorTitle + ": the SECONDHAND vehicle is missing registration data or kilometers");
        }
    }
}
