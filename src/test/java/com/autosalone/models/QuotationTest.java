package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    private Customer defaultCustomer;
    private Vehicle defaultCar;

    @BeforeEach
    public void setUp() {
        this.defaultCar = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setInShowroom(true)
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
        Quotation quote = new Quotation(defaultCar, defaultCustomer);

        assertAll(
                () -> assertNotNull(quote),
                () -> assertEquals(QuotationStatus.DRAFT, quote.getStatus()),
                () -> assertFalse(quote.isArchived()),
                () -> assertTrue(LocalDate.now().equals(quote.getDate())),
                () -> assertNull(quote.getExpirationDate()),
                () -> assertNull(quote.getExpirationPolicy()),
                () -> assertEquals(defaultCustomer, quote.getCustomer()),
                () -> assertEquals(defaultCar, quote.getVehicle()),
                () -> assertTrue(new BigDecimal("10000.00").equals(quote.getVehicleSellingPriceSnapshot())),
                () -> assertTrue(quote.getItems().size() == 0),
                () -> assertTrue(BigDecimal.ZERO.equals(quote.getAdditionalFees())),
                () -> assertTrue(BigDecimal.ZERO.equals(quote.getDiscountAmount())),
                () -> assertNull(quote.getPublicNotes()),
                () -> assertNull(quote.getInternalNotes()));
    }

    @Test
    public void constructor_WhenVehicleIsSold_ThrowsException() {
        defaultCar.setStatus(VehicleStatus.SOLD);

        assertThrows(IllegalStateException.class, () -> new Quotation(defaultCar, defaultCustomer),
                "Cannot create a quotation from a SOLD vehicle");
    }

    @Test
    public void constructor_WhenVehicleIsReserved_ThrowsException() {
        defaultCar.setStatus(VehicleStatus.RESERVED);

        assertThrows(IllegalStateException.class, () -> new Quotation(defaultCar, defaultCustomer),
                "Cannot create a quotation from a RESERVED vehicle");
    }

    @Test
    public void constructor_WhenVehicleIsQuoted_Success() {
        defaultCar.setStatus(VehicleStatus.QUOTED);

        assertDoesNotThrow(() -> new Quotation(defaultCar, defaultCustomer),
                "Should be able to create a quotation from a QUOTED vehicle");
    }

    // cloning copy construsctor
    @Test
    public void copyConstructor_FromDraftQuotation_Success() {
        LocalDate date = LocalDate.now().plusDays(10);
        Quotation sourceQuote = new Quotation(defaultCar, defaultCustomer);
        sourceQuote.setDate(date);
        sourceQuote.setExpirationPolicy10Days();
        sourceQuote.setAdditionalFees(new BigDecimal("300.00"));

        Quotation quote = new Quotation(sourceQuote);

        assertAll(
                () -> assertEquals(QuotationStatus.DRAFT, quote.getStatus()),
                () -> assertFalse(quote.isArchived()),
                () -> assertTrue(LocalDate.now().equals(quote.getDate())),
                () -> assertNull(quote.getExpirationDate()),
                () -> assertNull(quote.getExpirationPolicy()),
                () -> assertEquals(defaultCar, quote.getVehicle()),
                () -> assertEquals(defaultCustomer, quote.getCustomer()),
                () -> assertTrue(new BigDecimal("300.00").equals(quote.getAdditionalFees())),
                () -> assertTrue(new BigDecimal("10000.00").equals(sourceQuote.getVehicleSellingPriceSnapshot())));

    }

    @Test
    public void copyConstructor_FromArchivedQuotation_Success() {
        Quotation sourceQuote = new Quotation(defaultCar, defaultCustomer);
        sourceQuote.archive();
        assertTrue(sourceQuote.isArchived());

        Quotation quote = new Quotation(sourceQuote);

        assertFalse(quote.isArchived(), "The cloned quotation should not be archived");
    }

    @Test
    public void copyConstructor_FromIssuedQuotation_Success() {
        Quotation sourceQuote = new Quotation(defaultCar, defaultCustomer);
        sourceQuote.setExpirationDate(LocalDate.now());
        sourceQuote.issue();
        assertEquals(QuotationStatus.ISSUED, sourceQuote.getStatus());

        Quotation quote = new Quotation(sourceQuote);

        assertEquals(QuotationStatus.DRAFT, quote.getStatus(), "The cloned quotation should be DRAFT");
    }

    @Test
    public void copyConstructor_FromAcceptedQuotation_Success() {
        Quotation sourceQuote = new Quotation(defaultCar, defaultCustomer);
        sourceQuote.setExpirationDate(LocalDate.now());
        sourceQuote.issue();
        sourceQuote.accept();
        assertEquals(QuotationStatus.ACCEPTED, sourceQuote.getStatus());

        Quotation quote = new Quotation(sourceQuote);

        assertEquals(QuotationStatus.DRAFT, quote.getStatus(), "The cloned quotation should be DRAFT");
    }

    @Test
    public void copyConstructor_FromVoidedQuotation_Success() {
        Quotation sourceQuote = new Quotation(defaultCar, defaultCustomer);
        sourceQuote.voidDocument();
        assertEquals(QuotationStatus.VOIDED, sourceQuote.getStatus());

        Quotation quote = new Quotation(sourceQuote);

        assertEquals(QuotationStatus.DRAFT, quote.getStatus(), "The cloned quotation should be DRAFT");
    }

    @Test
    public void copyConstructor_FromExpiredQuotation_Success() {
        Quotation sourceQuote = new Quotation(defaultCar, defaultCustomer);
        sourceQuote.setExpirationDate(LocalDate.now());
        sourceQuote.issue();

        setPastExpirationDateForTesting(sourceQuote, 1);
        assertTrue(sourceQuote.isPastExpiration());
        assertEquals(QuotationStatus.EXPIRED, sourceQuote.getStatus());

        Quotation quote = new Quotation(sourceQuote);

        assertEquals(QuotationStatus.DRAFT, quote.getStatus(), "The cloned quotation should be DRAFT");
        assertNull(quote.getExpirationDate(), "The cloned quotation should have null expiration date");
        assertNull(quote.getExpirationPolicy(), "The cloned quotation should have null expiration policy");
    }

    @Test
    public void copyConstructor_WhenVehicleIsWithdrawn_ThrowsException() {
        Quotation sourceQuote = new Quotation(defaultCar, defaultCustomer);
        defaultCar.setStatus(VehicleStatus.WITHDRAWN);

        assertThrows(IllegalStateException.class, () -> new Quotation(sourceQuote));
    }

    @Test
    public void copyConstructor_AfterOriginalPricesHaveBeenUpdated_Success() {
        Quotation sourceQuote = new Quotation(defaultCar, defaultCustomer);
        Accessory accessory = new Accessory("Accessorio", null, new BigDecimal("50.00"));
        sourceQuote.addItem(new AppliedItem(accessory));

        accessory.setBasePrice(new BigDecimal("40.00"));
        defaultCar.setSellingPrice(new BigDecimal("15000.00"));

        Quotation quote = new Quotation(sourceQuote);

        assertAll(
                () -> assertTrue(sourceQuote.getItems().size() == 1),
                () -> assertTrue(quote.getItems().size() == 1),
                () -> assertTrue(new BigDecimal("10000.00").equals(sourceQuote.getVehicleSellingPriceSnapshot())),
                () -> assertTrue(new BigDecimal("15000.00").equals(quote.getVehicleSellingPriceSnapshot()),
                        "The cloned quotation should get the current vehicle selling price"),
                () -> assertTrue(new BigDecimal("50.00").equals(sourceQuote.getItems().getFirst().getAppliedPrice())),
                () -> assertTrue(new BigDecimal("40.00").equals(quote.getItems().getFirst().getAppliedPrice()),
                        "The cloned quotation should get the current item base price"));
    }

    @Test
    public void copyConstructor_AfterOriginalItemHasBeenArchived_Success() {
        Quotation sourceQuote = new Quotation(defaultCar, defaultCustomer);
        Accessory accessory = new Accessory("Accessorio", null, new BigDecimal("50.00"));
        sourceQuote.addItem(new AppliedItem(accessory));
        accessory.archive();

        Quotation quote = new Quotation(sourceQuote);

        assertTrue(quote.getItems().isEmpty(),
                "The cloned quotation should not contain archived items");

    }

    @Test
    public void changeExpirationDate_WhenStatusDraft_ExpirationPolicyBecomesCustom() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setDate(LocalDate.of(2096, 02, 05));
        quote.setExpirationPolicyEndOfMonth();

        quote.setExpirationDate(LocalDate.of(2096, 03, 01));

        assertEquals(ExpirationPolicy.CUSTOM, quote.getExpirationPolicy());
        assertTrue(LocalDate.of(2096, 03, 01).equals(quote.getExpirationDate()));

    }

    @Test
    public void changeDate_WhenStatusDraft_WithEndOfMonthExpirationPolicy_ExpirationDateIsRecalculated() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setDate(LocalDate.of(2096, 02, 05)); // Date: 5 February 2096 (leap year)
        quote.setExpirationPolicyEndOfMonth(); // End of month => Expiration date: 29 February 2096

        assertEquals(ExpirationPolicy.END_OF_MONTH, quote.getExpirationPolicy());
        assertTrue(LocalDate.of(2096, 02, 29).equals(quote.getExpirationDate()));

        quote.setDate(LocalDate.of(2096, 03, 05)); // Date: 5 March 2096
        // End of month => Expiration date: 31 March 2096

        assertEquals(ExpirationPolicy.END_OF_MONTH, quote.getExpirationPolicy());
        assertTrue(LocalDate.of(2096, 03, 31).equals(quote.getExpirationDate()));
    }

    @Test
    public void changeDate_WhenStatusDraft_WithTenDaysExpirationPolicy_ExpirationDateIsRecalculated() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setDate(LocalDate.of(2096, 02, 20)); // Date: 20 February 2096 (leap year)
        quote.setExpirationPolicy10Days(); // Ten days => Expiration date: 1 March 2096

        assertEquals(ExpirationPolicy.TEN_DAYS, quote.getExpirationPolicy());
        assertTrue(LocalDate.of(2096, 03, 01).equals(quote.getExpirationDate()));

        quote.setDate(LocalDate.of(2096, 03, 05)); // Date: 5 March 2096
        // Ten days => Expiration date: 15 March 2096

        assertEquals(ExpirationPolicy.TEN_DAYS, quote.getExpirationPolicy());
        assertTrue(LocalDate.of(2096, 03, 15).equals(quote.getExpirationDate()));
    }

    @Test
    public void setVehicleSellingPriceSnapshot_WhenStatusDraft_ActualVehicleSellingPriceDoesNotChange() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        assertTrue(new BigDecimal("10000.00").equals(quote.getVehicleSellingPriceSnapshot()));

        quote.setVehicleSellingPriceSnapshot(new BigDecimal("9800.00"));
        assertTrue(new BigDecimal("9800.00").equals(quote.getVehicleSellingPriceSnapshot()));
        assertTrue(new BigDecimal("10000.00").equals(quote.getVehicle().getSellingPrice()));
    }

    @Test
    public void issue_WhenStatusExpired_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        setPastExpirationDateForTesting(quote, 1);

        assertThrows(IllegalStateException.class, () -> {
            quote.issue();
        }, "Cannot issue an EXPIRED quotation (the expiration date is already in the past)");
    }

    @Test
    public void issue_WhenVehicleIsSold_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        defaultCar.setStatus(VehicleStatus.SOLD);

        assertThrows(IllegalStateException.class, () -> {
            quote.issue();
        }, "Cannot issue a quotation from a SOLD vehicle");
    }

    @Test
    public void issue_WhenVehicleIsReserved_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        defaultCar.setStatus(VehicleStatus.RESERVED);

        assertThrows(IllegalStateException.class, () -> {
            quote.issue();
        }, "Cannot issue a quotation from a RESERVED vehicle");
    }

    @Test
    public void issue_WhenVehicleIsWithdrawn_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        defaultCar.setStatus(VehicleStatus.WITHDRAWN);

        assertThrows(IllegalStateException.class, () -> {
            quote.issue();
        }, "Cannot issue a quotation from a WITHDRAWN vehicle");
    }

    @Test
    public void issue_WhenVehicleIsQuoted_Success() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        defaultCar.setStatus(VehicleStatus.QUOTED);

        assertThrows(IllegalStateException.class, () -> {
            quote.issue();
        }, "Should be able to issue a quotation from a QUOTED vehicle");
    }

    @Test
    public void accept_WhenStatusDraft_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);

        assertThrows(IllegalStateException.class, () -> {
            quote.accept();
        }, "Cannot accept a DRAFT quotation");
        assertEquals(QuotationStatus.DRAFT, quote.getStatus());
    }

    @Test
    public void accept_WhenStatusExpired_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        quote.issue();
        setPastExpirationDateForTesting(quote, 1);

        assertEquals(QuotationStatus.EXPIRED, quote.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            quote.accept();
        }, "Cannot accept an EXPIRED quotation");
    }

    @Test
    public void accept_WhenStatusVoided_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        quote.issue();
        quote.voidDocument();

        assertThrows(IllegalStateException.class, () -> {
            quote.accept();
        }, "Cannot accept a VOIDED quotation");
        assertEquals(QuotationStatus.VOIDED, quote.getStatus());
    }

    @Test
    public void voidDocument_WhenStatusAccepted_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        quote.issue();
        quote.accept();

        assertThrows(IllegalStateException.class, () -> {
            quote.voidDocument();
        }, "Cannot void an ACCEPTED quotation");
        assertEquals(QuotationStatus.ACCEPTED, quote.getStatus());
    }

    @Test
    public void voidDocument_WhenStatusDraft_Success() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);

        assertDoesNotThrow(() -> {
            quote.voidDocument();
        }, "Should be able to void an ACCEPTED quotation");
        assertEquals(QuotationStatus.VOIDED, quote.getStatus());
    }

    @Test
    public void voidDocument_WhenStatusIssued_Success() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        quote.issue();

        assertDoesNotThrow(() -> {
            quote.voidDocument();
        }, "Should be able to void an ACCEPTED quotation");
        assertEquals(QuotationStatus.VOIDED, quote.getStatus());
    }

    // add item
    @Test
    public void addItem_WhenStatusDraft_Success() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);

        assertDoesNotThrow(() -> {
            AppliedItem accessoryItem = new AppliedItem(
                    new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
            quote.addItem(accessoryItem);
        }, "Should be able to add items to a DRAFT quotation");
    }

    @Test
    public void addItem_WhenStatusIssued_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        quote.issue();

        assertThrows(IllegalStateException.class, () -> {
            AppliedItem accessoryItem = new AppliedItem(
                    new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
            quote.addItem(accessoryItem);
        }, "Cannot add items to an ISSUED quotation");
    }

    @Test
    public void addItem_WhenStatusAccepted_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        quote.issue();
        quote.accept();

        assertThrows(IllegalStateException.class, () -> {
            AppliedItem accessoryItem = new AppliedItem(
                    new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
            quote.addItem(accessoryItem);
        }, "Cannot add items to an ACCEPTED quotation");
    }

    @Test
    public void addItem_WhenStatusExpired_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        quote.issue();
        setPastExpirationDateForTesting(quote, 1);

        assertThrows(IllegalStateException.class, () -> {
            AppliedItem accessoryItem = new AppliedItem(
                    new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
            quote.addItem(accessoryItem);
        }, "Cannot add items to an EXPIRED quotation");
    }

    @Test
    public void addItem_WhenStatusVoided_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.voidDocument();

        assertThrows(IllegalStateException.class, () -> {
            AppliedItem accessoryItem = new AppliedItem(
                    new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
            quote.addItem(accessoryItem);
        }, "Cannot add items to a VOIDED quotation");
    }

    // remove item
    @Test
    public void removeItem_WhenStatusDraft_Success() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
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
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue();

        assertThrows(IllegalStateException.class, () -> {
            quote.removeItem(accessoryItem);
        }, "Cannot remove items from an ISSUED quotation");
    }

    @Test
    public void removeItem_WhenStatusAccepted_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue();
        quote.accept();

        assertThrows(IllegalStateException.class, () -> {
            quote.removeItem(accessoryItem);
        }, "Cannot remove items from an ACCEPTED quotation");
    }

    @Test
    public void removeItem_WhenStatusExpired_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue();
        setPastExpirationDateForTesting(quote, 1);

        assertThrows(IllegalStateException.class, () -> {
            quote.removeItem(accessoryItem);
        }, "Cannot remove items from an EXPIRED quotation");
    }

    @Test
    public void removeItem_WhenStatusVoided_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.voidDocument();

        assertThrows(IllegalStateException.class, () -> {
            quote.removeItem(accessoryItem);
        }, "Cannot remove items from a VOIDED quotation");
    }

    // set item price
    @Test
    public void setAppliedItemPrice_WhenStatusDraft_ToValidPrice_Success() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);

        assertDoesNotThrow(() -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("980.00"));
        }, "Should be able to change the applied price of items in a DRAFT quotation");
        assertTrue(new BigDecimal("980.00").equals(accessoryItem.getAppliedPrice()));
        assertTrue(new BigDecimal("1000.00").equals(accessoryItem.getItem().getPrice()),
                "Should not apply the change to the base price of the item");
    }

    @Test
    public void setAppliedItemPrice_WhenStatusDraft_ToNullPrice_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);

        assertThrows(NullPointerException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, null);
        });
    }

    @Test
    public void setAppliedItemPrice_WhenStatusDraft_ToNegativePrice_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);

        assertThrows(IllegalArgumentException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("-1000.00"));
        });
    }

    @Test
    public void setAppliedItemPrice_WhenStatusIssued_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue();

        assertThrows(IllegalStateException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("950.00"));
        }, "Cannot change items price of an ISSUED quotation");
    }

    @Test
    public void setAppliedItemPrice_WhenStatusAccepted_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue();
        quote.accept();

        assertThrows(IllegalStateException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("950.00"));
        }, "Cannot change items price of an ACCEPTED quotation");
    }

    @Test
    public void setAppliedItemPrice_WhenStatusExpired_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.issue();
        setPastExpirationDateForTesting(quote, 1);

        assertThrows(IllegalStateException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("950.00"));
        }, "Cannot change items price of an EXPIRED quotation");
    }

    @Test
    public void setAppliedItemPrice_WhenStatusVoided_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        AppliedItem accessoryItem = new AppliedItem(
                new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00")));
        quote.addItem(accessoryItem);
        quote.voidDocument();

        assertThrows(IllegalStateException.class, () -> {
            quote.setAppliedItemPrice(accessoryItem, new BigDecimal("950.00"));
        }, "Cannot change items price of a VOIDED quotation");
    }

    // get final price
    @Test
    public void getFinalPrice_PercentageDiscountAndFees_CalculatesCorrectly() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
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
        assertTrue(new BigDecimal("10400.00").equals(finalPrice),
                "The final price should strictly exempt additional fees from the discount strategy");
    }

    @Test
    public void getFinalPrice_PercentageDiscountAndFeesWithDecimals_RoundsHalfUpCorrectly() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        AppliedItem accessoryItem = new AppliedItem(new Accessory("Sensori", null, new BigDecimal("501.50")));
        quote.addItem(accessoryItem);
        quote.setAdditionalFees(new BigDecimal("250.00"));
        quote.setDiscountStrategy(new PercentageDiscountStrategy(new BigDecimal(15)));

        BigDecimal finalPrice = quote.getFinalPrice();

        // subtotal = 10,000 + 501.50 = 10,501.50
        // discount (15%) = 1,575.225 => HALF_UP must round to 1,575.23
        // discounted subtotal = 10,501.50 - 1,575.23 = 8,926.27
        // total (plus additional fees) = 8,926.27 + 250 = 9,176.27
        assertTrue(new BigDecimal("9176.27").equals(finalPrice),
                "Should round .225 up to .23 and calculate total price accurately");
    }

    @Test
    public void getFinalPrice_FixedDiscountExceedsSubtotal_DiscountAsSubtotal() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setAdditionalFees(new BigDecimal("300.00"));
        quote.setDiscountStrategy(new FixedAmountDiscountStrategy(new BigDecimal("50000.00")));

        BigDecimal finalPrice = quote.getFinalPrice();

        // subtotal = 10,000
        // discount (fixed) 50,000 > 10,000 => discount = subtotal = 10,000
        // discounted subtotal = 10,000 - 10,000 = 0
        // total (plus additional fees) = 0 + 300 = 300
        assertTrue(new BigDecimal("300.00").equals(finalPrice),
                "If the fixed discount is higher than the vehicle selling price, the subtotal becomes zero, but fees remain");
    }

    // archive
    @Test
    public void archive_WhenStatusDraft_Success() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now().plusMonths(1));

        assertEquals(QuotationStatus.DRAFT, quote.getStatus());

        quote.archive();
        assertTrue(quote.isArchived(), "Should be able to archive a DRAFT quotation");
    }

    @Test
    public void archive_WhenStatusExpired_Success() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        setPastExpirationDateForTesting(quote, 1);

        quote.archive();

        assertTrue(quote.isArchived(), "Should be able to archive an EXPIRED quotation");
        assertEquals(QuotationStatus.DRAFT, quote.getStatus(),
                "A DRAFT quotation with expiration date in the past should still be DRAFT");
    }

    @Test
    public void archive_WhenStatusAccepted_Success() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        quote.issue();
        quote.accept();

        quote.archive();

        assertTrue(quote.isArchived(), "Should be able to archive an ACCEPTED quotation");
    }

    @Test
    public void archive_WhenStatusVoided_Success() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.voidDocument();

        quote.archive();

        assertTrue(quote.isArchived(), "Should be able to archive a VOIDED quotation");
    }

    @Test
    public void archive_WhenStatusIssued_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        quote.issue();

        assertThrows(IllegalStateException.class, () -> quote.archive(), "Cannot archive an ISSUED quotation");

        assertFalse(quote.isArchived());
    }

    // helper method
    private void setPastExpirationDateForTesting(Quotation quote, int daysInPast) {
        try {
            Field field = Quotation.class.getDeclaredField("expirationDate");
            field.setAccessible(true);
            field.set(quote, LocalDate.now().minusDays(daysInPast));
        } catch (Exception e) {
            throw new RuntimeException("Failed to set past date via reflection for testing", e);
        }
    }
}
