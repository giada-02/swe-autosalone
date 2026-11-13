package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.autosalone.enums.ContractStatus;
import com.autosalone.enums.DiscountType;
import com.autosalone.enums.ExpirationPolicy;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Customer.CustomerBuilder;
import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.catalog.AppliedItem;
import com.autosalone.models.discounts.PercentageDiscountStrategy;

public class ContractTest {

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
                .setFiscalCode("RSSMRA00X00X000X")
                .setResidenceCity("Roma")
                .setZipCode("00000")
                .build();
    }

    // base constructor
    @Test
    public void constructor_WhenVehicleIsReserved_ThrowsException() {
        defaultCar.setStatus(VehicleStatus.RESERVED);

        assertThrows(IllegalStateException.class, () -> {
            new Contract(defaultCar, defaultCustomer, TODAY);
        }, "Cannot create a contract from a RESERVED vehicle");
    }

    @Test
    public void constructor_WhenVehicleIsSold_ThrowsException() {
        defaultCar.setStatus(VehicleStatus.SOLD);

        assertThrows(IllegalStateException.class, () -> {
            new Contract(defaultCar, defaultCustomer, TODAY);
        }, "Cannot create a contract from a SOLD vehicle");
    }

    @Test
    public void constructor_WhenVehicleIsWithdrawn_ThrowsException() {
        defaultCar.withdraw("Furto");

        assertThrows(IllegalStateException.class, () -> {
            new Contract(defaultCar, defaultCustomer, TODAY);
        }, "Cannot create a contract from a WITHDRAWN vehicle");
    }

    @Test
    public void constructor_WhenVehicleIsQuoted_Success() {
        defaultCar.setStatus(VehicleStatus.QUOTED);
        assertDoesNotThrow(() -> new Contract(defaultCar, defaultCustomer, TODAY));
    }

    @Test
    public void constructor_WhenVehicleIsAvailable_Success() {
        Contract contract = assertDoesNotThrow(() -> new Contract(defaultCar, defaultCustomer, TODAY));

        assertAll(
                () -> assertNotNull(contract),
                () -> assertEquals(ContractStatus.DRAFT, contract.getStatus()),
                () -> assertFalse(contract.isArchived()),
                () -> assertEquals(TODAY, contract.getDate()),
                () -> assertNull(contract.getDeposit()),
                () -> assertTrue(contract.getPayments().isEmpty()),
                () -> assertNull(contract.getQuotationReference()),
                () -> assertNull(contract.getEstimatedHandoverDate()),
                () -> assertNull(contract.getCancelationReason()),
                () -> assertEquals(defaultCustomer, contract.getCustomer()),
                () -> assertEquals(defaultCar, contract.getVehicle()),
                () -> assertEquals(0, contract.getVehicleSellingPriceSnapshot().compareTo(new BigDecimal("10000.00"))),
                () -> assertTrue(contract.getItems().size() == 0),
                () -> assertEquals(0, contract.getAdditionalFees().compareTo(BigDecimal.ZERO)),
                () -> assertEquals(0, contract.getDiscountAmount().compareTo(BigDecimal.ZERO)),
                () -> assertNull(contract.getPublicNotes()),
                () -> assertNull(contract.getInternalNotes()));
    }

    // conversion copy constructor
    @Test
    public void copyConstructor_FromDraftQuotation_ThrowsException() {
        Quotation quote = getSourceQuotation();

        assertThrows(IllegalStateException.class, () -> {
            new Contract(quote, TODAY);
        }, "Cannot create a contract from a DRAFT quotation");
    }

    @Test
    public void copyConstructor_FromAcceptedQuotation_ThrowsException() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        quote.accept(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            new Contract(quote, TODAY);
        }, "Cannot create a contract from an ACCEPTED quotation");
    }

    @Test
    public void copyConstructor_FromExpiredQuotation_ThrowsException() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            new Contract(quote, TODAY.plusDays(1));
        }, "Cannot create a contract from an EXPIRED quotation");
    }

    @Test
    public void copyConstructor_FromVoidedQuotation_ThrowsException() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        quote.voidDocument(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            new Contract(quote, TODAY);
        }, "Cannot create a contract from a VOIDED quotation");
    }

    @Test
    public void copyConstructor_FromArchivedQuotation_ThrowsException() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        quote.archive(TODAY);

        assertThrows(IllegalStateException.class, () -> {
            new Contract(quote, TODAY);
        }, "Cannot create a contract from an archived quotation");
    }

    @Test
    public void copyConstructor_FromValidIssuedQuotation_Success_CreatesDraftContract() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY.plusDays(10));
        quote.setAdditionalFees(new BigDecimal("350.00"));
        quote.setDiscountStrategy(new PercentageDiscountStrategy(new BigDecimal("10")));
        quote.setPublicNotes("Note pubbliche");
        quote.setInternalNotes("Note interne");
        quote.issue(TODAY);

        Contract contract = assertDoesNotThrow(() -> new Contract(quote, TODAY),
                "Should be able to create a contract from an ISSUED quotation");

        assertAll(
                () -> assertNotNull(contract),
                () -> assertEquals(ContractStatus.DRAFT, contract.getStatus()),
                () -> assertFalse(contract.isArchived()),
                () -> assertEquals(TODAY, contract.getDate()),
                () -> assertNull(contract.getDeposit()),
                () -> assertTrue(contract.getPayments().isEmpty()),
                () -> assertEquals(quote, contract.getQuotationReference()),
                () -> assertNull(contract.getEstimatedHandoverDate()),
                () -> assertNull(contract.getCancelationReason()),
                () -> assertEquals(defaultCustomer, contract.getCustomer()),
                () -> assertEquals(defaultCar, contract.getVehicle()),
                () -> assertEquals(0, contract.getVehicleSellingPriceSnapshot().compareTo(new BigDecimal("10000.00"))),
                () -> assertEquals(0,
                        quote.getVehicleSellingPriceSnapshot().compareTo(contract.getVehicleSellingPriceSnapshot())),
                () -> assertTrue(contract.getItems().size() == 0),
                () -> assertEquals(0, contract.getAdditionalFees().compareTo(new BigDecimal("350.00"))),
                () -> assertEquals(0, quote.getAdditionalFees().compareTo(contract.getAdditionalFees())),
                () -> assertEquals(quote.getDiscountStrategy(), contract.getDiscountStrategy()),
                () -> assertEquals(0, contract.getDiscountAmount().compareTo(new BigDecimal("1000.00"))),
                () -> assertEquals(0, quote.getDiscountAmount().compareTo(contract.getDiscountAmount())),
                () -> assertEquals(quote.getPublicNotes(), contract.getPublicNotes()),
                () -> assertEquals(quote.getInternalNotes(), contract.getInternalNotes()));
    }

    @Test
    public void copyConstructor_WhenVehicleIsReserved_ThrowsException() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        defaultCar.setStatus(VehicleStatus.RESERVED);

        assertThrows(IllegalStateException.class, () -> {
            new Contract(quote, TODAY);
        }, "Cannot create a contract from a quotation with a RESERVED vehicle");
    }

    @Test
    public void copyConstructor_WhenVehicleIsSold_ThrowsException() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        defaultCar.setStatus(VehicleStatus.SOLD);

        assertThrows(IllegalStateException.class, () -> {
            new Contract(quote, TODAY);
        }, "Cannot create a contract from a quotation with a SOLD vehicle");
    }

    @Test
    public void copyConstructor_WhenVehicleIsWithdrawn_ThrowsException() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        defaultCar.withdraw("Restituito al fornitore");

        assertThrows(IllegalStateException.class, () -> {
            new Contract(quote, TODAY);
        }, "Cannot create a contract from a quotation with a WITHDRAWN vehicle");
    }

    @Test
    public void copyConstructor_WhenVehicleIsQuoted_Success() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        defaultCar.setStatus(VehicleStatus.QUOTED);

        assertDoesNotThrow(() -> new Contract(quote, TODAY));
    }

    @Test
    public void copyConstructor_AfterOriginalPricesHaveBeenUpdatedAndItemHasBeenArchived_Success() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        Accessory accessory = new Accessory("Accessorio", null, new BigDecimal("50.00"));
        quote.addItem(new AppliedItem(accessory));
        quote.issue(TODAY);

        accessory.setBasePrice(new BigDecimal("40.00"));
        defaultCar.setSellingPrice(new BigDecimal("15000.00"));

        Contract contract = new Contract(quote, TODAY);

        assertAll(
                () -> assertTrue(contract.getItems().size() == 1),
                () -> assertEquals(0, contract.getVehicleSellingPriceSnapshot().compareTo(new BigDecimal("10000.00")),
                        "The contract created from a quotation should get the snapshot vehicle selling price"),
                () -> assertEquals(0,
                        contract.getItems().getFirst().getAppliedPrice().compareTo(new BigDecimal("50.00")),
                        "The contract created from a quotation should get the snapshot applied price of the item"));

    }

    @Test
    public void copyConstructor_AfterOriginalItemHasBeenArchived_Success() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        Accessory accessory = new Accessory("Accessorio", null, new BigDecimal("50.00"));
        quote.addItem(new AppliedItem(accessory));
        quote.issue(TODAY);

        accessory.archive();

        Contract contract = new Contract(quote, TODAY);

        assertTrue(contract.getItems().size() == 1,
                "The contract created from a quotation should have all items of the original quotation source even if they have been archived");
    }

    @Test
    public void copyConstructor_CopiesDiscountFields() {
        Quotation quote = getSourceQuotation();
        quote.setDiscountStrategy(new PercentageDiscountStrategy(new BigDecimal("10")));
        quote.updateExpiration(ExpirationPolicy.TEN_DAYS);
        quote.issue(TODAY);

        Contract contract = new Contract(quote, TODAY);

        assertAll(
                () -> assertEquals(DiscountType.PERCENTAGE, contract.getDiscountType()),
                () -> assertNotNull(contract.getDiscountValue()),
                () -> assertEquals(0, new BigDecimal("10").compareTo(contract.getDiscountValue())),
                () -> assertEquals(quote.getDiscountStrategy(), contract.getDiscountStrategy()));
    }

    @Test
    public void changeCustomer_WhenStatusDraft_Success() {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);
        Customer newCustomer = new CustomerBuilder().setFirstName("Luigi").setLastName("Verdi")
                .setPhoneNumber("1234567890")
                .build();

        assertDoesNotThrow(() -> contract.setCustomer(newCustomer));
        assertEquals(newCustomer, contract.getCustomer());
    }

    @Test
    public void changeCustomer_WhenStatusDraft_CreatedFromQuotation_ThrowsException() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        Contract contract = new Contract(quote, TODAY);
        Customer newCustomer = new CustomerBuilder().setFirstName("Luigi").setLastName("Verdi")
                .setPhoneNumber("1234567890")
                .build();

        assertThrows(IllegalStateException.class, () -> contract.setCustomer(newCustomer));
    }

    @Test
    public void changeVehicle_WhenStatusDraft_CreatedFromQuotation_ThrowsException() {
        Quotation quote = getSourceQuotation();
        quote.updateExpiration(TODAY);
        quote.issue(TODAY);
        Contract contract = new Contract(quote, TODAY);
        Vehicle newVehicle = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Giallo")
                .setIsInShowroom(true)
                .setCondition(VehicleCondition.SECONDHAND)
                .setSellingPrice(new BigDecimal("9000.00"))
                .build();

        assertThrows(IllegalStateException.class, () -> contract.setVehicle(newVehicle));
    }

    @Test
    public void applyChanges_WhenStatusConfirmed_ThrowsException() {
        Contract contract = getConfirmedContract("500.00");
        assertDocumentIsLocked(contract);
    }

    @Test
    public void applyChanges_WhenStatusCompleted_ThrowsException() {
        Contract contract = getCompletedContract();
        assertDocumentIsLocked(contract);
    }

    @Test
    public void applyChanges_WhenStatusCanceled_ThrowsException() {
        Contract contract = getConfirmedContract("500.00");
        contract.cancel("Cancellazione per Finanziamento Rifiutato");

        assertDocumentIsLocked(contract);
    }

    @Test
    public void setInternalNotes_WhenStatusConfirmed_Success() {
        Contract contract = getConfirmedContract("500.00");

        assertDoesNotThrow(() -> {
            contract.setInternalNotes("Il cliente ha chiamato per chiedere info sulla consegna.");
        }, "Internal notes should remain editable even when the contract is CONFIRMED");

        assertEquals("Il cliente ha chiamato per chiedere info sulla consegna.", contract.getInternalNotes());
    }

    @Test
    public void setInternalNotes_WhenStatusCompleted_Success() {
        Contract contract = getCompletedContract();

        assertDoesNotThrow(() -> {
            contract.setInternalNotes("Pratica archiviata, tutto ok.");
        });

        assertEquals("Pratica archiviata, tutto ok.", contract.getInternalNotes());
    }

    @Test
    public void cancel_WhenStatusDraft_ThrowsException() {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);

        assertEquals(ContractStatus.DRAFT, contract.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            contract.cancel("Cancellazione per Finanziamento Rifiutato");
        }, "Cannot cancel a DRAFT contract");
    }

    @Test
    public void cancel_WhenStatusCompleted_ThrowsException() {
        Contract contract = getCompletedContract();

        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            contract.cancel("Rinuncia volontaria");
        }, "Cannot cancel a COMPLETED contract");
    }

    @Test
    public void cancel_WhenStatusConfirmed_Success() {
        Contract contract = getConfirmedContract("500.00");

        contract.cancel("Ripensamento");

        assertEquals(ContractStatus.CANCELED, contract.getStatus(), "Should be able to cancel a CONFIRMED contract");
    }

    @Test
    public void cancel_WhenStatusConfirmed_WithoutCancelationReason_ThrowsException() {
        Contract contract = getConfirmedContract("500.00");

        assertAll("Canceling a contract without a cancelation reason should throw exceptions",
                () -> assertThrows(IllegalArgumentException.class, () -> contract.cancel(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> contract.cancel("")));
    }

    @Test
    public void confirm_WhenStausDraft_WithoutEstimatedHandoverDate_ThrowsException() {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);

        assertThrows(IllegalStateException.class, () -> {
            contract.confirm(null);
        });
        assertEquals(ContractStatus.DRAFT, contract.getStatus());
    }

    @Test
    public void confirm_WhenStatusDraft_WithoutCustomerFiscalData_ThrowsException() {
        Customer incompleteCustomer = new CustomerBuilder().setFirstName("Mario")
                .setLastName("Rossi")
                .setPhoneNumber("1234567890")
                .setResidenceCity("Roma")
                .setZipCode("00000")
                .build();
        Contract contract = new Contract(defaultCar, incompleteCustomer, TODAY);

        assertThrows(IllegalStateException.class, () -> {
            contract.confirm(null);
        });
        assertEquals(ContractStatus.DRAFT, contract.getStatus());
    }

    @Test
    public void confirm_WhenStatusDraft_WithoutCustomerResidenceCity_ThrowsException() {
        Customer incompleteCustomer = new CustomerBuilder().setFirstName("Mario")
                .setLastName("Rossi")
                .setPhoneNumber("1234567890")
                .setFiscalCode("RSSMRA00X00X000X")
                .setZipCode("00000")
                .build();
        Contract contract = new Contract(defaultCar, incompleteCustomer, TODAY);

        assertThrows(IllegalStateException.class, () -> {
            contract.confirm(null);
        });
        assertEquals(ContractStatus.DRAFT, contract.getStatus());
    }

    @Test
    public void confirm_WhenStatusDraft_WithoutCustomerZipCode_ThrowsException() {
        Customer incompleteCustomer = new CustomerBuilder().setFirstName("Mario")
                .setLastName("Rossi")
                .setPhoneNumber("1234567890")
                .setFiscalCode("RSSMRA00X00X000X")
                .setResidenceCity("Roma")
                .build();
        Contract contract = new Contract(defaultCar, incompleteCustomer, TODAY);

        assertThrows(IllegalStateException.class, () -> {
            contract.confirm(null);
        });
        assertEquals(ContractStatus.DRAFT, contract.getStatus());
    }

    @Test
    public void confirm_WhenStatusDraft_WithDepositOverTotal_ThrowsException() {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);
        contract.setEstimatedHandoverDate(TODAY);

        BigDecimal depositAmount = contract.getFinalPrice().add(new BigDecimal("1000.00"));
        Transaction deposit = TransactionFactory.createContractDeposit(contract, depositAmount, TODAY);

        assertThrows(IllegalArgumentException.class, () -> {
            contract.confirm(deposit);
        });
        assertEquals(ContractStatus.DRAFT, contract.getStatus());
    }

    @Test
    public void confirm_WhenStatusDraft_WithDepositEqualTotal_Success() {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);
        contract.setEstimatedHandoverDate(TODAY);

        BigDecimal contractTotal = contract.getFinalPrice();
        Transaction deposit = TransactionFactory.createContractDeposit(contract, contractTotal, TODAY);

        assertDoesNotThrow(() -> {
            contract.confirm(deposit);
        });
        assertEquals(ContractStatus.CONFIRMED, contract.getStatus());
    }

    @Test
    public void confirm_WhenStatusDraft_WithNullDepositTransaction_Success() {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);
        contract.setEstimatedHandoverDate(TODAY);

        assertDoesNotThrow(() -> {
            contract.confirm(null);
        });

        assertAll(
                () -> assertEquals(ContractStatus.CONFIRMED, contract.getStatus()),
                () -> assertNull(contract.getDeposit()),
                () -> assertEquals(defaultCustomer.getFirstName(), contract.getCustomerSnapshot().getFirstName()),
                () -> assertEquals(defaultCustomer.getLastName(), contract.getCustomerSnapshot().getLastName()),
                () -> assertEquals(defaultCustomer.getFiscalCode(), contract.getCustomerSnapshot().getFiscalCode()),
                () -> assertNull(defaultCustomer.getVatNumber()),
                () -> assertNull(contract.getCustomerSnapshot().getVatNumber()),
                () -> assertEquals(
                        defaultCustomer.getResidenceCity(), contract.getCustomerSnapshot().getResidenceCity()),
                () -> assertEquals(defaultCustomer.getZipCode(), contract.getCustomerSnapshot().getZipCode()),
                () -> assertNull(defaultCustomer.getEmail()),
                () -> assertNull(contract.getCustomerSnapshot().getEmail()),
                () -> assertEquals(defaultCustomer.getPhoneNumber(), contract.getCustomerSnapshot().getPhoneNumber()));
    }

    @Test
    public void confirm_WhenStatusVoided_ThrowsException() {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);
        contract.setEstimatedHandoverDate(TODAY);
        contract.voidDocument();

        assertThrows(IllegalStateException.class, () -> {
            contract.confirm(null);
        }, "Cannot confirm a VOIDED contract");
    }

    @Test
    public void voidDocument_WhenStatusConfirmed_ThrowsException() {
        Contract contract = getConfirmedContract(null);

        assertThrows(IllegalStateException.class, () -> {
            contract.voidDocument();
        }, "Cannot void a CONFIRMED contract");
    }

    @Test
    public void voidDocument_WhenStatusCompleted_ThrowsException() {
        Contract contract = getCompletedContract();

        assertThrows(IllegalStateException.class, () -> {
            contract.voidDocument();
        }, "Cannot void a COMPLETED contract");
    }

    @Test
    public void voidDocument_WhenStatusCanceled_ThrowsException() {
        Contract contract = getConfirmedContract(null);
        contract.cancel("Finanziamento Rifiutato");

        assertThrows(IllegalStateException.class, () -> {
            contract.voidDocument();
        }, "Cannot void a CANCELED contract");
    }

    @Test
    public void voidDocument_WhenStatusDraft_Success() {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);

        assertDoesNotThrow(() -> {
            contract.voidDocument();
        }, "Should be able to void a DRAFT contract");
        assertEquals(ContractStatus.VOIDED, contract.getStatus());
    }

    @Test
    public void registerPayment_WhenStatusDraft_ThrowsException() {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);
        Transaction payment = TransactionFactory.createContractPayment(contract, null, new BigDecimal("500.00"), TODAY);

        assertEquals(ContractStatus.DRAFT, contract.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            contract.registerPayment(payment);
        }, "Cannot accept payments for a DRAFT contract");
    }

    @Test
    public void registerPayment_WhenStatusCompleted_ThrowsException() {
        Contract contract = getCompletedContract();
        Transaction payment = TransactionFactory.createContractPayment(contract, null, new BigDecimal("500.00"), TODAY);

        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            contract.registerPayment(payment);
        }, "Cannot accept payments for a COMPLETED contract");
    }

    @Test
    public void registerPayment_WhenStatusCanceled_ThrowsException() {
        Contract contract = getConfirmedContract("500");
        contract.cancel("Ripensamento");
        Transaction payment = TransactionFactory.createContractPayment(contract, null, new BigDecimal("500.00"), TODAY);

        assertEquals(ContractStatus.CANCELED, contract.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            contract.registerPayment(payment);
        }, "Cannot accept payments for a CANCELED contract");
    }

    @Test
    public void registerRefund_WhenStatusDraft_ThrowsException() {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);
        Transaction refund = TransactionFactory.createContractRefund(contract, null, new BigDecimal("500.00"), TODAY);

        assertEquals(ContractStatus.DRAFT, contract.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            contract.registerRefund(refund);
        }, "Cannot process refunds for a DRAFT contract");
    }

    @Test
    public void registerRefund_WhenStatusCompleted_ThrowsException() {
        Contract contract = getCompletedContract();
        Transaction refund = TransactionFactory.createContractRefund(contract, null, new BigDecimal("500.00"), TODAY);

        assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        assertThrows(IllegalStateException.class, () -> {
            contract.registerRefund(refund);
        }, "Cannot process refunds for a COMPLETED contract");
    }

    @Test
    public void registerRefund_WhenStatusConfirmed_Success() {
        Contract contract = getConfirmedContract("500.00");
        Transaction refund = TransactionFactory.createContractRefund(contract, null, new BigDecimal("500.00"), TODAY);

        assertEquals(ContractStatus.CONFIRMED, contract.getStatus());
        assertDoesNotThrow(() -> {
            contract.registerRefund(refund);
        }, "Should be able to process refunds for a CONFIRMED contract");
    }

    @Test
    public void registerRefund_WhenStatusCanceled_Success() {
        Contract contract = getConfirmedContract("500.00");
        Transaction refund = TransactionFactory.createContractRefund(contract, null, new BigDecimal("500.00"), TODAY);
        contract.cancel("Ripensamento");

        assertEquals(ContractStatus.CANCELED, contract.getStatus());
        assertDoesNotThrow(() -> {
            contract.registerRefund(refund);
        }, "Should be able to process refunds for a CANCELED contract");
    }

    @Test
    public void registerRefund_WhenAmountExceedsTotalPaid_ThrowsException() {
        Contract contract = getConfirmedContract("1000.00");
        Transaction payment = TransactionFactory.createContractPayment(contract, null, new BigDecimal("200.00"), TODAY);
        contract.registerPayment(payment);

        Transaction refund = TransactionFactory.createContractRefund(contract, null, new BigDecimal("1500.00"), TODAY);

        assertEquals(ContractStatus.CONFIRMED, contract.getStatus());
        assertThrows(IllegalArgumentException.class, () -> {
            contract.registerRefund(refund);
        }, "Cannot refund more money than what the customer has paid");
    }

    @Test
    public void registerRefund_WhenValid_DecreasesTotalPayment() {
        Contract contract = getConfirmedContract("1000.00");

        Transaction p1 = TransactionFactory.createContractPayment(contract, "Acconto", new BigDecimal("3000.00"),
                TODAY);
        contract.registerPayment(p1);

        assertEquals(0, contract.getTotalPayment().compareTo(new BigDecimal("4000.00")));
        assertEquals(0, contract.getFinalPrice().compareTo(new BigDecimal("10000.00")));
        assertEquals(0, contract.getRemainingBalance().compareTo(new BigDecimal("6000.00")));

        Transaction refund = TransactionFactory.createContractRefund(contract, null, new BigDecimal("3000.00"), TODAY);
        contract.registerRefund(refund);

        assertEquals(2, contract.getPayments().size(), "List should contain both IN and OUT transactions");
        assertEquals(0, contract.getTotalPayment().compareTo(new BigDecimal("1000.00")),
                "Total payment should be reduced by the refund");
        assertEquals(0, contract.getFinalPrice().compareTo(new BigDecimal("10000.00")));
        assertEquals(0, contract.getRemainingBalance().compareTo(new BigDecimal("9000.00")),
                "Remaining balance should increase after a refund");
    }

    @Test
    public void registerPayment_WhenAmountExceedsBalance_ThrowsException() {
        Contract contract = getConfirmedContract("1000.00");

        Transaction overPayment = TransactionFactory.createContractPayment(contract, "Acconto",
                new BigDecimal("15000.00"), TODAY);

        assertThrows(IllegalArgumentException.class, () -> {
            contract.registerPayment(overPayment);
        }, "Should throw exception if payment exceeds the remaining balance");
    }

    @Test
    public void registerPayment_WhenValid_UpdatesTotalAndBalance() {
        Contract contract = getConfirmedContract("1000.00");

        Transaction p1 = TransactionFactory.createContractPayment(contract, "Acconto 1", new BigDecimal("2000.00"),
                TODAY);
        Transaction p2 = TransactionFactory.createContractPayment(contract, "Acconto 2", new BigDecimal("3000.00"),
                TODAY);

        contract.registerPayment(p1);
        contract.registerPayment(p2);

        assertEquals(2, contract.getPayments().size());
        assertEquals(0, contract.getTotalPayment().compareTo(new BigDecimal("6000.00")));
        assertEquals(0, contract.getFinalPrice().compareTo(new BigDecimal("10000.00")));
        assertEquals(0, contract.getRemainingBalance().compareTo(new BigDecimal("4000.00")));
    }

    // archive
    @Test
    public void archive_WhenStatusDraft_Success() {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);

        contract.archive(TODAY);
        assertTrue(contract.isArchived(), "Should be able to archive a DRAFT contract");
    }

    @Test
    public void archive_WhenStatusConfirmed_ThrowsException() {
        Contract contract = getConfirmedContract("1000.00");

        assertThrows(IllegalStateException.class, () -> contract.archive(TODAY), "Cannot archive a CONFIRMED contract");
    }

    @Test
    public void archive_WhenStatusCompleted_Success() {
        Contract contract = getCompletedContract();

        contract.archive(TODAY);

        assertTrue(contract.isArchived(), "Should be able to archive a COMPLETED contract");
    }

    @Test
    public void archive_WhenStatusCanceled_Success() {
        Contract contract = getConfirmedContract("1000.00");
        contract.cancel("Finanziamento Rifiutato");

        contract.archive(TODAY);

        assertTrue(contract.isArchived(), "Should be able to archive a CANCELED contract");
    }

    // helper methods

    private void assertDocumentIsLocked(SalesDocument document) {
        Customer newCustomer = new CustomerBuilder().setFirstName("Luigi").setLastName("Verdi")
                .setPhoneNumber("1234567890")
                .build();
        Vehicle newVehicle = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Giallo")
                .setIsInShowroom(true)
                .setCondition(VehicleCondition.SECONDHAND)
                .setSellingPrice(new BigDecimal("9000.00"))
                .build();

        assertAll("Editing a locked document should throw exceptions",
                () -> assertThrows(IllegalStateException.class, () -> document.setCustomer(newCustomer), "Customer"),
                () -> assertThrows(IllegalStateException.class, () -> document.setVehicle(newVehicle), "Vehicle"),
                () -> assertThrows(IllegalStateException.class,
                        () -> document.setAdditionalFees(new BigDecimal("300.00")), "Fees"),
                () -> assertThrows(IllegalStateException.class,
                        () -> document.setDiscountStrategy(new PercentageDiscountStrategy(new BigDecimal("10"))),
                        "Discount"),
                () -> assertThrows(IllegalStateException.class, () -> document.setDate(TODAY.plusDays(1)),
                        "Date"),
                () -> assertThrows(IllegalStateException.class,
                        () -> document.setVehicleSellingPriceSnapshot(new BigDecimal("12000.00")), "Price Snapshot"),
                () -> assertThrows(IllegalStateException.class, () -> document.setPublicNotes("Note"), "Public Notes"));
    }

    private Contract getCompletedContract() {
        Contract contract = getConfirmedContract("500.00");
        Transaction remainingAmout = TransactionFactory.createContractPayment(contract, "Rimanente",
                new BigDecimal("9500.00"), TODAY);
        contract.registerPayment(remainingAmout);
        defaultCar.setHandoverDate(TODAY);
        defaultCar.setLicensePlate("AB123CD");
        defaultCar.setRegistrationDate(TODAY);
        contract.complete();
        return contract;
    }

    private Contract getConfirmedContract(String depositValue) {
        Contract contract = new Contract(defaultCar, defaultCustomer, TODAY);
        Transaction deposit = null;
        if (depositValue != null)
            deposit = TransactionFactory.createContractDeposit(contract, new BigDecimal(depositValue),
                    TODAY);
        contract.setEstimatedHandoverDate(
                TODAY);
        contract.confirm(deposit);
        return contract;
    }

    private Quotation getSourceQuotation() {
        return new Quotation(defaultCar, defaultCustomer, TODAY);
    }
}
