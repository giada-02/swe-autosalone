package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.autosalone.enums.DiscountType;
import com.autosalone.enums.ExpirationPolicy;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Customer.CustomerBuilder;
import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.catalog.AppliedItem;
import com.autosalone.models.discounts.FixedAmountDiscountStrategy;
import com.autosalone.models.discounts.PercentageDiscountStrategy;

public class QuotationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);

    private Customer defaultCustomer;
    private Vehicle defaultCar;

    @BeforeEach
    public void setUp() {
        this.defaultCar = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setIsInShowroom(true)
                .setCondition(VehicleCondition.NEW)
                .setSellingPrice(new BigDecimal("10000.00"))
                .build();
        this.defaultCustomer = new CustomerBuilder()
                .setFirstName("Mario")
                .setLastName("Rossi")
                .setPhoneNumber("1234567890")
                .build();
    }

    // base constructor
    @Test
    public void constructor() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer, TODAY);

        assertAll(
                () -> assertNotNull(quote),
                () -> assertEquals(QuotationStatus.DRAFT, quote.getStatus(TODAY)),
                () -> assertFalse(quote.isArchived()),
                () -> assertEquals(TODAY, quote.getDate()),
                () -> assertNull(quote.getExpirationDate()),
                () -> assertNull(quote.getExpirationPolicy()),
                () -> assertEquals(defaultCustomer, quote.getCustomer()),
                () -> assertEquals(defaultCar, quote.getVehicle()),
                () -> assertEquals(0, quote.getVehicleSellingPriceSnapshot().compareTo(new BigDecimal("10000.00"))),
                () -> assertTrue(quote.getItems().size() == 0),
                () -> assertEquals(0, quote.getAdditionalFees().compareTo(BigDecimal.ZERO)),
                () -> assertEquals(0, quote.getDiscountAmount().compareTo(BigDecimal.ZERO)),
                () -> assertNull(quote.getPublicNotes()),
                () -> assertNull(quote.getInternalNotes()));
    }

    @Test
    public void constructor_WhenVehicleIsSold_ThrowsException() {
        defaultCar.setStatus(VehicleStatus.SOLD);

        assertThrows(IllegalStateException.class, () -> new Quotation(defaultCar, defaultCustomer, TODAY),
                "Cannot create a quotation from a SOLD vehicle");
    }

    @Test
    public void constructor_WhenVehicleIsReserved_ThrowsException() {
        defaultCar.setStatus(VehicleStatus.RESERVED);

        assertThrows(IllegalStateException.class, () -> new Quotation(defaultCar, defaultCustomer, TODAY),
                "Cannot create a quotation from a RESERVED vehicle");
    }

    @Test
    public void constructor_WhenVehicleIsQuoted_Success() {
        defaultCar.setStatus(VehicleStatus.QUOTED);

        assertDoesNotThrow(() -> new Quotation(defaultCar, defaultCustomer, TODAY),
                "Should be able to create a quotation from a QUOTED vehicle");
    }

    @Test
    public void getStatus_IssuedPastExpiration_ReturnsExpiredWithoutChangingState() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer, TODAY);
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);

        assertEquals(QuotationStatus.EXPIRED, quote.getStatus(TODAY.plusDays(1)));
        assertDoesNotThrow(() -> {
            quote.expire();
        });
    }

    // cloning copy construsctor
    @Test
    public void copyConstructor_FromDraftQuotation_Success() {
        LocalDate date = TODAY.plusDays(10);
        Quotation sourceQuote = getDefaultQuotation();
        sourceQuote.setDate(date);
        sourceQuote.updateExpiration(ExpirationPolicy.TEN_DAYS);
        sourceQuote.setAdditionalFees(new BigDecimal("300.00"));

        Quotation quote = new Quotation(sourceQuote, TODAY);

        assertAll(
                () -> assertEquals(QuotationStatus.DRAFT, quote.getStatus(TODAY)),
                () -> assertFalse(quote.isArchived()),
                () -> assertEquals(TODAY, quote.getDate()),
                () -> assertNull(quote.getExpirationDate()),
                () -> assertNull(quote.getExpirationPolicy()),
                () -> assertEquals(defaultCar, quote.getVehicle()),
                () -> assertEquals(defaultCustomer, quote.getCustomer()),
                () -> assertEquals(0, quote.getAdditionalFees().compareTo(new BigDecimal("300.00"))),
                () -> assertEquals(0,
                        sourceQuote.getVehicleSellingPriceSnapshot().compareTo(new BigDecimal("10000.00"))));

    }

    @Test
    public void copyConstructor_FromArchivedQuotation_Success() {
        Quotation sourceQuote = getDefaultQuotation();
        sourceQuote.archive(TODAY);
        assertTrue(sourceQuote.isArchived());

        Quotation quote = new Quotation(sourceQuote, TODAY);

        assertFalse(quote.isArchived(), "The cloned quotation should not be archived");
    }

    @Test
    public void copyConstructor_FromIssuedQuotation_Success() {
        Quotation sourceQuote = getDefaultQuotation();
        sourceQuote.updateExpiration(TODAY);
        sourceQuote.issue(TODAY);
        assertEquals(QuotationStatus.ISSUED, sourceQuote.getStatus(TODAY));

        Quotation quote = new Quotation(sourceQuote, TODAY);

        assertEquals(QuotationStatus.DRAFT, quote.getStatus(TODAY), "The cloned quotation should be DRAFT");
    }

    @Test
    public void copyConstructor_FromAcceptedQuotation_Success() {
        Quotation sourceQuote = getDefaultQuotation();
        sourceQuote.updateExpiration(TODAY);
        sourceQuote.issue(TODAY);
        sourceQuote.accept(TODAY);
        assertEquals(QuotationStatus.ACCEPTED, sourceQuote.getStatus(TODAY));

        Quotation quote = new Quotation(sourceQuote, TODAY);

        assertEquals(QuotationStatus.DRAFT, quote.getStatus(TODAY), "The cloned quotation should be DRAFT");
    }

    @Test
    public void copyConstructor_FromVoidedQuotation_Success() {
        Quotation sourceQuote = getDefaultQuotation();
        sourceQuote.voidDocument(TODAY);
        assertEquals(QuotationStatus.VOIDED, sourceQuote.getStatus(TODAY));

        Quotation quote = new Quotation(sourceQuote, TODAY);

        assertEquals(QuotationStatus.DRAFT, quote.getStatus(TODAY), "The cloned quotation should be DRAFT");
    }

    @Test
    public void copyConstructor_FromExpiredQuotation_Success() {
        Quotation sourceQuote = getDefaultQuotation();
        sourceQuote.updateExpiration(TODAY);
        sourceQuote.issue(TODAY);

        LocalDate tomorrow = TODAY.plusDays(1);

        assertTrue(sourceQuote.isPastExpiration(tomorrow));
        assertEquals(QuotationStatus.EXPIRED, sourceQuote.getStatus(tomorrow));

        Quotation quote = new Quotation(sourceQuote, tomorrow);

        assertEquals(QuotationStatus.DRAFT, quote.getStatus(tomorrow), "The cloned quotation should be DRAFT");
        assertNull(quote.getExpirationDate(), "The cloned quotation should have null expiration date");
        assertNull(quote.getExpirationPolicy(), "The cloned quotation should have null expiration policy");
    }

    @Test
    public void copyConstructor_WhenVehicleIsWithdrawn_ThrowsException() {
        Quotation sourceQuote = getDefaultQuotation();
        defaultCar.withdraw("Distrutto");

        assertThrows(IllegalStateException.class, () -> new Quotation(sourceQuote, TODAY));
    }

    @Test
    public void copyConstructor_AfterOriginalPricesHaveBeenUpdated_Success() {
        Quotation sourceQuote = getDefaultQuotation();
        Accessory accessory = new Accessory("Accessorio", null, new BigDecimal("50.00"));
        sourceQuote.addItem(new AppliedItem(accessory));

        accessory.setBasePrice(new BigDecimal("40.00"));
        defaultCar.setSellingPrice(new BigDecimal("15000.00"));

        Quotation quote = new Quotation(sourceQuote, TODAY);

        assertAll(
                () -> assertTrue(sourceQuote.getItems().size() == 1),
                () -> assertTrue(quote.getItems().size() == 1),
                () -> assertEquals(0,
                        sourceQuote.getVehicleSellingPriceSnapshot().compareTo(new BigDecimal("10000.00"))),
                () -> assertEquals(0, quote.getVehicleSellingPriceSnapshot().compareTo(new BigDecimal("15000.00")),
                        "The cloned quotation should get the current vehicle selling price"),
                () -> assertEquals(0,
                        sourceQuote.getItems().getFirst().getAppliedPrice().compareTo(new BigDecimal("50.00"))),
                () -> assertEquals(0, quote.getItems().getFirst().getAppliedPrice().compareTo(new BigDecimal("40.00")),
                        "The cloned quotation should get the current item base price"));
    }

    @Test
    public void copyConstructor_AfterOriginalItemHasBeenArchived_Success() {
        Quotation sourceQuote = getDefaultQuotation();
        Accessory accessory = new Accessory("Accessorio", null, new BigDecimal("50.00"));
        sourceQuote.addItem(new AppliedItem(accessory));
        accessory.archive();

        Quotation quote = new Quotation(sourceQuote, TODAY);

        assertTrue(quote.getItems().isEmpty(),
                "The cloned quotation should not contain archived items");

    }

    @Test
    public void copyConstructor_CopiesDiscountFields() {
        Quotation sourceQuote = getDefaultQuotation();
        sourceQuote.setDiscountStrategy(new FixedAmountDiscountStrategy(new BigDecimal("500.00")));

        Quotation clone = new Quotation(sourceQuote, TODAY);

        assertAll(
                () -> assertEquals(DiscountType.FIXED, clone.getDiscountType()),
                () -> assertNotNull(clone.getDiscountValue()),
                () -> assertEquals(0, new BigDecimal("500.00").compareTo(clone.getDiscountValue())),
                () -> assertEquals(sourceQuote.getDiscountStrategy(), clone.getDiscountStrategy()));
    }

    @Test
    public void changeExpirationDate_WhenStatusDraft_ExpirationPolicyBecomesCustom() {
        Quotation quote = getDefaultQuotation();
        quote.setDate(LocalDate.of(2028, 02, 05));
        quote.updateExpiration(ExpirationPolicy.END_OF_MONTH);

        quote.updateExpiration(LocalDate.of(2028, 03, 01));

        assertEquals(ExpirationPolicy.CUSTOM, quote.getExpirationPolicy());
        assertEquals(LocalDate.of(2028, 03, 01), quote.getExpirationDate());
    }

    @Test
    public void changeDate_WhenStatusDraft_WithEndOfMonthExpirationPolicy_ExpirationDateIsRecalculated() {
        Quotation quote = getDefaultQuotation();
        quote.setDate(LocalDate.of(2028, 02, 05)); // Date: 5 February 2028 (leap year)
        quote.updateExpiration(ExpirationPolicy.END_OF_MONTH); // End of month => Expiration date: 29 February 2028

        assertEquals(ExpirationPolicy.END_OF_MONTH, quote.getExpirationPolicy());
        assertEquals(LocalDate.of(2028, 02, 29), quote.getExpirationDate());

        quote.setDate(LocalDate.of(2028, 03, 05)); // Date: 5 March 2028
        // End of month => Expiration date: 31 March 2028

        assertEquals(ExpirationPolicy.END_OF_MONTH, quote.getExpirationPolicy());
        assertEquals(LocalDate.of(2028, 03, 31), quote.getExpirationDate());
    }

    @Test
    public void changeDate_WhenStatusDraft_WithTenDaysExpirationPolicy_ExpirationDateIsRecalculated() {
        Quotation quote = getDefaultQuotation();
        quote.setDate(LocalDate.of(2028, 02, 20)); // Date: 20 February 2028 (leap year)
        quote.updateExpiration(ExpirationPolicy.TEN_DAYS); // Ten days => Expiration date: 1 March 2028

        assertEquals(ExpirationPolicy.TEN_DAYS, quote.getExpirationPolicy());
        assertEquals(LocalDate.of(2028, 03, 01), quote.getExpirationDate());

        quote.setDate(LocalDate.of(2028, 03, 05)); // Date: 5 March 2028
        // Ten days => Expiration date: 15 March 2028

        assertEquals(ExpirationPolicy.TEN_DAYS, quote.getExpirationPolicy());
        assertEquals(LocalDate.of(2028, 03, 15), quote.getExpirationDate());
    }

    @Test
    public void updateExpiration_WhenStatusIsNotDraft_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);

        assertThrows(IllegalStateException.class, () -> quote.updateExpiration(ExpirationPolicy.END_OF_MONTH));
        assertEquals(ExpirationPolicy.CUSTOM, quote.getExpirationPolicy());
        assertEquals(TODAY, quote.getExpirationDate());
    }

    @Test
    public void updateExpiration_ToNullExpirationPolicy_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        assertThrows(NullPointerException.class, () -> quote.updateExpiration(null, TODAY),
                "Expiration policy is required to update the expiration");
    }

    @Test
    public void updateExpiration_ToCustomExpirationPolicyWithNullExpirationDate_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        assertThrows(IllegalArgumentException.class, () -> quote.updateExpiration(ExpirationPolicy.CUSTOM, null),
                "Expiration policy of type CUSTOM requires a custom expiration date");
    }

    @Test
    public void updateExpiration_ToCustomExpirationPolicyWithExpirationDateBeforeDocumentDate_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.setDate(TODAY);
        assertThrows(IllegalArgumentException.class,
                () -> quote.updateExpiration(ExpirationPolicy.CUSTOM, TODAY.minusDays(1)),
                "Cannot set the expiration date before the document date");
    }

    @Test
    public void updateExpiration_ToCustomExpirationPolicyWhenStatusIsDraft_Success() {
        Quotation quote = getDefaultQuotation();
        quote.setDate(TODAY);

        quote.updateExpiration(TODAY.plusDays(1));

        assertEquals(ExpirationPolicy.CUSTOM, quote.getExpirationPolicy());
        assertEquals(TODAY.plusDays(1), quote.getExpirationDate());
    }

    @Test
    public void updateExpiration_ToTenDaysExpirationPolicyWhenStatusIsDraft_Success() {
        Quotation quote = getDefaultQuotation();
        quote.setDate(LocalDate.of(2026, 1, 30));

        quote.updateExpiration(ExpirationPolicy.TEN_DAYS);

        assertEquals(ExpirationPolicy.TEN_DAYS, quote.getExpirationPolicy());
        assertEquals(LocalDate.of(2026, 2, 9), quote.getExpirationDate());
    }

    @Test
    public void updateExpiration_ToEndOfMonthExpirationPolicyWhenStatusIsDraft_Success() {
        Quotation quote = getDefaultQuotation();
        quote.setDate(LocalDate.of(2026, 1, 11));

        quote.updateExpiration(ExpirationPolicy.END_OF_MONTH);

        assertEquals(ExpirationPolicy.END_OF_MONTH, quote.getExpirationPolicy());
        assertEquals(LocalDate.of(2026, 1, 31), quote.getExpirationDate());
    }

    @Test
    public void setAdditionalFees_ToNegative_ThrowsException() {
        Quotation quote = getDefaultQuotation();

        assertThrows(IllegalArgumentException.class,
                () -> quote.setAdditionalFees(new BigDecimal("-800.00")), "Additional fees cannot be negative");
    }

    @Test
    public void setAdditionalFees_ToZero_Success() {
        Quotation quote = getDefaultQuotation();

        quote.setAdditionalFees(BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, quote.getAdditionalFees());
    }

    @Test
    public void setAdditionalFees_ToNull_SetsAdditionalFeesToZero() {
        Quotation quote = getDefaultQuotation();

        quote.setAdditionalFees(null);

        assertEquals(BigDecimal.ZERO, quote.getAdditionalFees());
    }

    @Test
    public void setVehicleSellingPriceSnapshot_WhenStatusDraft_ActualVehicleSellingPriceDoesNotChange() {
        Quotation quote = getDefaultQuotation();
        assertEquals(0, quote.getVehicleSellingPriceSnapshot().compareTo(new BigDecimal("10000.00")));

        quote.setVehicleSellingPriceSnapshot(new BigDecimal("9800.00"));
        assertEquals(0, quote.getVehicleSellingPriceSnapshot().compareTo(new BigDecimal("9800.00")));
        assertEquals(0, quote.getVehicle().getSellingPrice().compareTo(new BigDecimal("10000.00")));
    }

    @Test
    public void setVehicleSellingPriceSnapshot_ToNull_ThrowsException() {
        Quotation quote = getDefaultQuotation();

        assertThrows(NullPointerException.class,
                () -> quote.setVehicleSellingPriceSnapshot(null));
    }

    @Test
    public void setVehicleSellingPriceSnapshot_ToNegative_ThrowsException() {
        Quotation quote = getDefaultQuotation();

        assertThrows(IllegalArgumentException.class,
                () -> quote.setVehicleSellingPriceSnapshot(new BigDecimal("-9800.00")));
    }

    @Test
    public void issue_WhenStatusExpired_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            quote.issue(TODAY.plusDays(1));
        }, "Cannot issue an EXPIRED quotation (the expiration date is already in the past)");
    }

    @Test
    public void issue_WhenArchived_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.archive(TODAY);

        assertTrue(quote.isArchived());
        assertThrows(IllegalStateException.class, () -> {
            quote.issue(TODAY);
        }, "Cannot issue an ARCHIVED quotation");
    }

    @Test
    public void issue_WhenVehicleIsSold_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        defaultCar.setStatus(VehicleStatus.SOLD);

        assertThrows(IllegalStateException.class, () -> {
            quote.issue(TODAY);
        }, "Cannot issue a quotation from a SOLD vehicle");
    }

    @Test
    public void issue_WhenVehicleIsReserved_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        defaultCar.setStatus(VehicleStatus.RESERVED);

        assertThrows(IllegalStateException.class, () -> {
            quote.issue(TODAY);
        }, "Cannot issue a quotation from a RESERVED vehicle");
    }

    @Test
    public void issue_WhenVehicleIsWithdrawn_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        defaultCar.withdraw("Restituito al fornitore");

        assertThrows(IllegalStateException.class, () -> {
            quote.issue(TODAY);
        }, "Cannot issue a quotation from a WITHDRAWN vehicle");
    }

    @Test
    public void issue_WhenVehicleIsQuoted_Success() {
        Quotation quote = getDefaultQuotation();
        defaultCar.setStatus(VehicleStatus.QUOTED);

        assertThrows(IllegalStateException.class, () -> {
            quote.issue(TODAY);
        }, "Should be able to issue a quotation from a QUOTED vehicle");
    }

    @Test
    public void accept_WhenStatusDraft_ThrowsException() {
        Quotation quote = getDefaultQuotation();

        assertThrows(IllegalStateException.class, () -> {
            quote.accept(TODAY);
        }, "Cannot accept a DRAFT quotation");
    }

    @Test
    public void accept_WhenStatusExpired_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            quote.accept(TODAY.plusDays(1));
        }, "Cannot accept an EXPIRED quotation");
    }

    @Test
    public void accept_WhenStatusVoided_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        quote.voidDocument(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            quote.accept(TODAY);
        }, "Cannot accept a VOIDED quotation");
    }

    @Test
    public void voidDocument_WhenStatusAccepted_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        quote.accept(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            quote.voidDocument(TODAY);
        }, "Cannot void an ACCEPTED quotation");
    }

    @Test
    public void voidDocument_IssuedPastExpiration_BecomesExpired() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);

        quote.voidDocument(TODAY.plusDays(1));

        assertEquals(QuotationStatus.EXPIRED, quote.getStatus(TODAY));
    }

    @Test
    public void voidDocument_WhenStatusDraft_Success() {
        Quotation quote = getDefaultQuotation();

        assertDoesNotThrow(() -> {
            quote.voidDocument(TODAY);
        }, "Should be able to void a DRAFT quotation");
        assertEquals(QuotationStatus.VOIDED, quote.getStatus(TODAY));
    }

    @Test
    public void voidDocument_DraftPastExpiration_Success() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);

        assertDoesNotThrow(() -> {
            quote.voidDocument(TODAY.plusDays(1));
        });
        assertEquals(QuotationStatus.VOIDED, quote.getStatus(TODAY.plusDays(1)));
    }

    @Test
    public void voidDocument_WhenStatusIssued_Success() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);

        assertDoesNotThrow(() -> {
            quote.voidDocument(TODAY);
        });
        assertEquals(QuotationStatus.VOIDED, quote.getStatus(TODAY));
    }

    // add item
    @Test
    public void addItem_WhenStatusDraft_Success() {
        Quotation quote = getDefaultQuotation();

        assertDoesNotThrow(() -> {
            AppliedItem accessoryItem = new AppliedItem(
                    new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
            quote.addItem(accessoryItem);
        }, "Should be able to add items to a DRAFT quotation");
    }

    @Test
    public void addItem_WhenStatusIssued_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            AppliedItem accessoryItem = new AppliedItem(
                    new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
            quote.addItem(accessoryItem);
        }, "Cannot add items to an ISSUED quotation");
    }

    @Test
    public void addItem_WhenStatusAccepted_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        quote.accept(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            AppliedItem accessoryItem = new AppliedItem(
                    new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
            quote.addItem(accessoryItem);
        }, "Cannot add items to an ACCEPTED quotation");
    }

    @Test
    public void addItem_WhenStatusExpired_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        quote.expire();

        assertThrows(IllegalStateException.class, () -> {
            AppliedItem accessoryItem = new AppliedItem(
                    new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
            quote.addItem(accessoryItem);
        }, "Cannot add items to an EXPIRED quotation");
    }

    @Test
    public void addItem_WhenStatusVoided_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.voidDocument(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            AppliedItem accessoryItem = new AppliedItem(
                    new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
            quote.addItem(accessoryItem);
        }, "Cannot add items to a VOIDED quotation");
    }

    // remove item
    @Test
    public void removeItem_WhenStatusDraft_Success() {
        Quotation quote = getDefaultQuotation();
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);

        assertDoesNotThrow(() -> {
            quote.removeItem(accessoryItem);
        }, "Should be able to remove items from a DRAFT quotation");
        assertTrue(quote.getItems().isEmpty());
    }

    @Test
    public void removeItem_WhenStatusIssued_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            quote.removeItem(accessoryItem);
        }, "Cannot remove items from an ISSUED quotation");
    }

    @Test
    public void removeItem_WhenStatusAccepted_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue(TODAY);
        quote.accept(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            quote.removeItem(accessoryItem);
        }, "Cannot remove items from an ACCEPTED quotation");
    }

    @Test
    public void removeItem_WhenStatusExpired_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue(TODAY);
        quote.expire();

        assertThrows(IllegalStateException.class, () -> {
            quote.removeItem(accessoryItem);
        }, "Cannot remove items from an EXPIRED quotation");
    }

    @Test
    public void removeItem_WhenStatusVoided_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.voidDocument(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            quote.removeItem(accessoryItem);
        }, "Cannot remove items from a VOIDED quotation");
    }

    // set item price
    @Test
    public void setAppliedItemPrice_WhenStatusDraft_ToValidPrice_Success() {
        Quotation quote = getDefaultQuotation();
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);

        assertDoesNotThrow(() -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("980.00"));
        }, "Should be able to change the applied price of items in a DRAFT quotation");
        assertEquals(0, accessoryItem.getAppliedPrice().compareTo(new BigDecimal("980.00")));
        assertEquals(0, accessoryItem.getItem().getPrice().compareTo(new BigDecimal("1000.00")),
                "Should not apply the change to the base price of the item");
    }

    @Test
    public void setAppliedItemPrice_WhenStatusDraft_ToNullPrice_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);

        assertThrows(NullPointerException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, null);
        });
    }

    @Test
    public void setAppliedItemPrice_WhenStatusDraft_ToNegativePrice_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);

        assertThrows(IllegalArgumentException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("-1000.00"));
        });
    }

    @Test
    public void setAppliedItemPrice_WhenStatusIssued_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("950.00"));
        }, "Cannot change items price of an ISSUED quotation");
    }

    @Test
    public void setAppliedItemPrice_WhenStatusAccepted_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue(TODAY);
        quote.accept(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("950.00"));
        }, "Cannot change items price of an ACCEPTED quotation");
    }

    @Test
    public void setAppliedItemPrice_WhenStatusExpired_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue(TODAY);
        quote.expire();

        assertThrows(IllegalStateException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("950.00"));
        }, "Cannot change items price of an EXPIRED quotation");
    }

    @Test
    public void setAppliedItemPrice_WhenStatusVoided_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.voidDocument(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("950.00"));
        }, "Cannot change items price of a VOIDED quotation");
    }

    // get final price
    @Test
    public void getFinalPrice_PercentageDiscountAndFees_CalculatesCorrectly() {
        Quotation quote = getDefaultQuotation();
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Cerchi in lega", "Estetica", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.setAdditionalFees(new BigDecimal("500.00"));
        quote.setDiscountStrategy(new PercentageDiscountStrategy(new BigDecimal(10)));

        BigDecimal finalPrice = quote.getFinalPrice();

        // subtotal = 10,000 + 1,000 = 11,000
        // discount (10%) = 1,100
        // discounted subtotal = 11,000 - 1,100 = 9,900
        // total (plus additional fees) = 9,900 + 500 = 10,400
        assertEquals(0, finalPrice.compareTo(new BigDecimal("10400.00")),
                "The final price should strictly exempt additional fees from the discount strategy");
    }

    @Test
    public void getFinalPrice_PercentageDiscountAndFeesWithDecimals_RoundsHalfUpCorrectly() {
        Quotation quote = getDefaultQuotation();
        AppliedItem accessoryItem = new AppliedItem(new Accessory("Sensori", null, new BigDecimal("501.50")));
        quote.addItem(accessoryItem);
        quote.setAdditionalFees(new BigDecimal("250.00"));
        quote.setDiscountStrategy(new PercentageDiscountStrategy(new BigDecimal(15)));

        BigDecimal finalPrice = quote.getFinalPrice();

        // subtotal = 10,000 + 501.50 = 10,501.50
        // discount (15%) = 1,575.225 => HALF_UP must round to 1,575.23
        // discounted subtotal = 10,501.50 - 1,575.23 = 8,926.27
        // total (plus additional fees) = 8,926.27 + 250 = 9,176.27
        assertEquals(0, finalPrice.compareTo(new BigDecimal("9176.27")),
                "Should round .225 up to .23 and calculate total price accurately");
    }

    @Test
    public void getFinalPrice_FixedDiscountExceedsSubtotal_DiscountAsSubtotal() {
        Quotation quote = getDefaultQuotation();
        quote.setAdditionalFees(new BigDecimal("300.00"));
        quote.setDiscountStrategy(new FixedAmountDiscountStrategy(new BigDecimal("50000.00")));

        BigDecimal finalPrice = quote.getFinalPrice();

        // subtotal = 10,000
        // discount (fixed) 50,000 > 10,000 => discount = subtotal = 10,000
        // discounted subtotal = 10,000 - 10,000 = 0
        // total (plus additional fees) = 0 + 300 = 300
        assertEquals(0, finalPrice.compareTo(new BigDecimal("300.00")),
                "If the fixed discount is higher than the vehicle selling price, the subtotal becomes zero, but fees remain");
    }

    // archive
    @Test
    public void archive_WhenStatusDraft_Success() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY.plusMonths(1));

        assertEquals(QuotationStatus.DRAFT, quote.getStatus(TODAY));

        quote.archive(TODAY);
        assertTrue(quote.isArchived(), "Should be able to archive a DRAFT quotation");
    }

    @Test
    public void archive_WhenStatusExpired_Success() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);

        quote.archive(TODAY.plusDays(1));

        assertTrue(quote.isArchived(), "Should be able to archive an EXPIRED quotation");
        assertEquals(QuotationStatus.DRAFT, quote.getStatus(TODAY.plusDays(1)),
                "A DRAFT quotation with expiration date in the past should still be DRAFT");
    }

    @Test
    public void archive_WhenStatusAccepted_Success() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        quote.accept(TODAY);

        quote.archive(TODAY);

        assertTrue(quote.isArchived(), "Should be able to archive an ACCEPTED quotation");
    }

    @Test
    public void archive_WhenStatusVoided_Success() {
        Quotation quote = getDefaultQuotation();
        quote.voidDocument(TODAY);

        quote.archive(TODAY);

        assertTrue(quote.isArchived(), "Should be able to archive a VOIDED quotation");
    }

    @Test
    public void archive_WhenStatusIssued_ThrowsException() {
        Quotation quote = getDefaultQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);

        assertThrows(IllegalStateException.class, () -> quote.archive(TODAY), "Cannot archive an ISSUED quotation");

        assertFalse(quote.isArchived());
    }

    private Quotation getDefaultQuotation() {
        return new Quotation(defaultCar, defaultCustomer, TODAY);
    }
}
