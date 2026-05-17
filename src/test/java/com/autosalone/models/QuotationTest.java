package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.autosalone.enums.QuotationStatus;
import com.autosalone.models.Customer.CustomerBuilder;
import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.discounts.FixedAmountDiscountStrategy;
import com.autosalone.models.discounts.PercentageDiscountStrategy;

public class QuotationTest {

    private Customer defaultCustomer;
    private Vehicle defaultCar;

    @BeforeEach
    public void setUp() {
        this.defaultCustomer = new CustomerBuilder().build();
        this.defaultCar = new Vehicle.VehicleBuilder().build();
    }

    @Test
    public void addAccessoryItem_WhenQuotationIsIssued_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        quote.setStatus(QuotationStatus.ISSUED);

        assertThrows(IllegalStateException.class, () -> {
            Accessory accessory = new Accessory("Alloy Rims", "Sport", new BigDecimal("1000.00"));
            quote.addItem(accessory);
        }, "Cannot add items to an ISSUED quotation");
    }

    @Test
    public void getFinalPrice_PercentageDiscountAndFees_CalculatesCorrectly() {
        Vehicle car = new Vehicle.VehicleBuilder()
                .setSellingPrice(new BigDecimal("20000.00"))
                .build();
        Quotation quote = new Quotation(car, defaultCustomer);
        Accessory accessory = new Accessory("Cerchi in lega", "Estetica", new BigDecimal("1000.00"));
        quote.addItem(accessory);
        quote.setAdditionalFees(new BigDecimal("500.00"));
        quote.setDiscountStrategy(new PercentageDiscountStrategy(10));

        BigDecimal finalPrice = quote.getFinalPrice();

        // subtotal = 20,000 + 1,000 = 21,000
        // discount (10%) = 2,100
        // discounted subtotal = 18,900
        // total (plus additional fees) = 18,900 + 500 = 19,400
        assertTrue(new BigDecimal("19400.00").equals(finalPrice),
                "The final price should strictly exempt additional fees from the discount strategy");
    }

    @Test
    public void getFinalPrice_PercentageDiscountAndFeesWithDecimals_RoundsHalfUpCorrectly() {
        Vehicle car = new Vehicle.VehicleBuilder()
                .setSellingPrice(new BigDecimal("10000.00"))
                .build();
        Quotation quote = new Quotation(car, defaultCustomer);
        Accessory accessory = new Accessory("Sensori", null, new BigDecimal("501.50"));
        quote.addItem(accessory);
        quote.setAdditionalFees(new BigDecimal("250.00"));
        quote.setDiscountStrategy(new PercentageDiscountStrategy(15));

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
        Vehicle car = new Vehicle.VehicleBuilder()
                .setSellingPrice(new BigDecimal("2000.00"))
                .build();
        Quotation quote = new Quotation(car, defaultCustomer);
        quote.setAdditionalFees(new BigDecimal("300.00"));
        quote.setDiscountStrategy(new FixedAmountDiscountStrategy(new BigDecimal("5000.00")));

        BigDecimal finalPrice = quote.getFinalPrice();

        // subtotal = 2,000
        // discount (fixed) 5,000 > 2,000 => discount = subtotal = 2,000
        // discounted subtotal = 2,000 - 2,000 = 0
        // total (plus additional fees) = 0 + 300 = 300
        assertTrue(new BigDecimal("300.00").equals(finalPrice),
                "If the fixed discount is higher than the vehicle selling price, the subtotal becomes zero, but fees remain");
    }

    @Test
    public void setStatus_Issued_WhenQuotationIsExpired_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        setPastExpirationDateForTesting(quote, 1);

        assertThrows(IllegalStateException.class, () -> {
            quote.setStatus(QuotationStatus.ISSUED);
        }, "Cannot issue a quotation if the expiration date is already in the past");
    }

    @Test
    public void archive_WhenQuotationIsDraft_ArchivesSuccessfully() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now().plusMonths(1));

        assertEquals(QuotationStatus.DRAFT, quote.getStatus());

        quote.archive();
        assertTrue(quote.isArchived(), "Should be able to archive a DRAFT quotation");
    }

    @Test
    public void archive_WhenQuotationIsExpired_ArchivesSuccessfully() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        setPastExpirationDateForTesting(quote, 1);

        quote.archive();

        assertTrue(quote.isArchived(), "Should be able to archive an EXPIRED quotation");
        assertEquals(QuotationStatus.DRAFT, quote.getStatus(),
                "A DRAFT quotation with expiration date in the past should still be DRAFT");
    }

    @Test
    public void archive_WhenQuotationIsVisible_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now().plusDays(10));

        quote.setStatus(QuotationStatus.ISSUED);

        assertThrows(IllegalStateException.class, () -> {
            quote.archive();
        }, "Cannot archive a quotation that is currently visible to the customer. Must revoke visibility first");
    }

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
