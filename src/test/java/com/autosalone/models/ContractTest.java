package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.autosalone.enums.ContractStatus;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.models.Customer.CustomerBuilder;

public class ContractTest {

    private Customer defaultCustomer;
    private Vehicle defaultCar;

    @BeforeEach
    public void setUp() {
        this.defaultCustomer = new CustomerBuilder().build();
        this.defaultCar = new Vehicle.VehicleBuilder().build();
    }

    @Test
    public void setCustomer_WhenContractIsConfirmed_ThrowsException() {
        Customer customer = new CustomerBuilder().setFirstName("Mario").build();
        Contract contract = new Contract(defaultCar, customer);
        contract.setStatus(ContractStatus.CONFIRMED);

        Customer newCustomer = new CustomerBuilder().setFirstName("Luigi").build();

        assertThrows(IllegalStateException.class, () -> {
            contract.setCustomer(newCustomer);
        }, "Cannot change customer on a CONFIRMED contract");
    }

    @Test
    public void constructor_FromDraftQuotation_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);

        assertEquals(QuotationStatus.DRAFT, quote.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            new Contract(quote);
        }, "Cannot create a contract from a DRAFT quotation");
    }

    @Test
    public void constructor_FromExpiredQuotation_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now());
        quote.setStatus(QuotationStatus.ISSUED);
        setPastExpirationDateForTesting(quote, 1);
        quote.setStatus(QuotationStatus.EXPIRED);

        assertThrows(IllegalStateException.class, () -> {
            new Contract(quote);
        }, "Cannot create a contract from an EXPIRED quotation");
    }

    @Test
    public void constructor_FromArchivedQuotation_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.archive();

        assertEquals(true, quote.isArchived());
        assertThrows(IllegalStateException.class, () -> {
            new Contract(quote);
        }, "Cannot create a contract from an ARCHIVED quotation");
    }

    @Test
    public void constructor_FromValidQuotation_CreatesDraftContract() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now().plusDays(10));
        quote.setStatus(QuotationStatus.ISSUED);

        Contract contract = new Contract(quote);

        assertNotNull(contract);
        assertEquals(ContractStatus.DRAFT, contract.getStatus(), "The new contract must be in status DRAFT");
    }

    @Test
    public void setStatus_Cancelled_WhenContractIsDraft_ThrowsException() {
        Contract contract = new Contract(defaultCar, defaultCustomer);

        assertEquals(ContractStatus.DRAFT, contract.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            contract.setStatus(ContractStatus.CANCELLED);
        }, "Cannot cancel a DRAFT contract");
    }

    @Test
    public void setStatus_Cancelled_WhenContractIsCompleted_ThrowsException() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now().plusDays(10));
        quote.setStatus(QuotationStatus.ISSUED);

        Contract contract = new Contract(quote);
        contract.setStatus(ContractStatus.CONFIRMED);
        contract.setStatus(ContractStatus.COMPLETED);

        assertThrows(IllegalStateException.class, () -> {
            contract.setStatus(ContractStatus.CANCELLED);
        }, "Cannot cancel a COMPLETED contract");
    }

    @Test
    public void setStatus_Cancelled_WhenContractIsConfirmed_CancelsContract() {
        Quotation quote = new Quotation(defaultCar, defaultCustomer);
        quote.setExpirationDate(LocalDate.now().plusDays(10));
        quote.setStatus(QuotationStatus.ISSUED);

        Contract contract = new Contract(quote);
        contract.setStatus(ContractStatus.CONFIRMED);

        contract.setStatus(ContractStatus.CANCELLED);

        assertEquals(ContractStatus.CANCELLED, contract.getStatus(), "Should be able to cancel a CONFIRMED contract");
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

    @Test
    public void registerPayment_WhenStatusNotConfirmed_ThrowsException() {
        Contract contract = new Contract(defaultCar, defaultCustomer);
        Transaction payment = TransactionFactory.createContractPayment(contract, null, new BigDecimal("500.00"),
                LocalDate.now());

        assertEquals(ContractStatus.DRAFT, contract.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            contract.registerPayment(payment);
        }, "Cannot accept payments for a contract not in CONFIRMED status");
    }

    @Test
    public void registerRefund_WhenStatusNotConfirmed_ThrowsException() {
        Contract contract = new Contract(defaultCar, defaultCustomer);
        Transaction refund = TransactionFactory.createContractRefund(contract, new BigDecimal("500.00"),
                LocalDate.now());

        assertEquals(ContractStatus.DRAFT, contract.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            contract.registerRefund(refund);
        }, "Cannot process refunds for a contract not in CONFIRMED status");
    }

    @Test
    public void registerPayment_WhenAmountExceedsBalance_ThrowsException() {
        Vehicle car = new Vehicle.VehicleBuilder().setBrand("Fiat").setModel("Panda")
                .setSellingPrice(new BigDecimal("10000.00")).build();

        Contract contract = new Contract(car, defaultCustomer);
        contract.setStatus(ContractStatus.CONFIRMED);

        Transaction overPayment = TransactionFactory.createContractPayment(contract, "Acconto",
                new BigDecimal("15000.00"),
                LocalDate.now());

        assertThrows(IllegalArgumentException.class, () -> {
            contract.registerPayment(overPayment);
        }, "Should throw exception if payment exceeds the remaining balance");
    }

    @Test
    public void registerPayment_WhenValid_UpdatesTotalAndBalance() {
        Vehicle car = new Vehicle.VehicleBuilder().setBrand("Fiat").setModel("Panda")
                .setSellingPrice(new BigDecimal("10000.00")).build();
        Contract contract = new Contract(car, defaultCustomer);
        contract.setStatus(ContractStatus.CONFIRMED);

        Transaction p1 = TransactionFactory.createContractPayment(contract, "Acconto 1", new BigDecimal("2000.00"),
                LocalDate.now());
        Transaction p2 = TransactionFactory.createContractPayment(contract, "Acconto 2", new BigDecimal("3000.00"),
                LocalDate.now());

        contract.registerPayment(p1);
        contract.registerPayment(p2);

        assertEquals(2, contract.getPayments().size());
        assertTrue(new BigDecimal("5000.00").equals(contract.getTotalPayment()));
        assertTrue(new BigDecimal("5000.00").equals(contract.getRemainingBalance()));
    }

    @Test
    public void registerRefund_WhenAmountExceedsTotalPaid_ThrowsException() {
        Vehicle car = new Vehicle.VehicleBuilder().setBrand("Fiat").setModel("Panda")
                .setSellingPrice(new BigDecimal("10000.00")).build();
        Contract contract = new Contract(car, defaultCustomer);
        contract.setStatus(ContractStatus.CONFIRMED);

        Transaction payment = TransactionFactory.createContractPayment(contract, null, new BigDecimal("1000.00"),
                LocalDate.now());
        contract.registerPayment(payment);

        Transaction excessiveRefund = TransactionFactory.createContractRefund(contract, new BigDecimal("2000.00"),
                LocalDate.now());

        assertThrows(IllegalArgumentException.class, () -> {
            contract.registerRefund(excessiveRefund);
        }, "Cannot refund more money than what the customer has paid");
    }

    @Test
    public void registerRefund_WhenValid_DecreasesTotalPayment() {
        Vehicle car = new Vehicle.VehicleBuilder().setBrand("Fiat").setModel("Panda")
                .setSellingPrice(new BigDecimal("10000")).build();
        Contract contract = new Contract(car, defaultCustomer);
        contract.setStatus(ContractStatus.CONFIRMED);

        Transaction p1 = TransactionFactory.createContractPayment(contract, "Acconto", new BigDecimal("3000.00"),
                LocalDate.now());
        contract.registerPayment(p1);

        assertTrue(new BigDecimal("3000.00").equals(contract.getTotalPayment()));

        Transaction refund = TransactionFactory.createContractRefund(contract, new BigDecimal("1000.00"),
                LocalDate.now());
        contract.registerRefund(refund);

        assertEquals(2, contract.getPayments().size(), "List should contain both IN and OUT transactions");
        assertTrue(new BigDecimal("2000.00").equals(contract.getTotalPayment()),
                "Total payment should be reduced by the refund");
        assertTrue(new BigDecimal("8000.00").equals(contract.getRemainingBalance()),
                "Remaining balance should increase after a refund");
    }
}
