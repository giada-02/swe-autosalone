package com.autosalone.repositories;

import com.autosalone.enums.DiscountType;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.models.Customer;
import com.autosalone.models.Quotation;
import com.autosalone.models.Vehicle;
import com.autosalone.models.discounts.DiscountStrategy;
import com.autosalone.models.discounts.PercentageDiscountStrategy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class QuotationRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);

    private EntityManagerFactory emf;
    private EntityManager em;
    private QuotationRepository repository;

    @BeforeEach
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("autosalonePU-test");
        em = emf.createEntityManager();
        repository = new QuotationRepository();
        repository.em = this.em;
    }

    @AfterEach
    public void tearDown() {
        if (em != null)
            em.close();
        if (emf != null)
            emf.close();
    }

    @Test
    public void saveQuotation_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Quotation quotation = new Quotation(vehicle, customer, TODAY);

        repository.save(quotation);
        em.getTransaction().commit();
        assertNotNull(quotation.getId());

        em.clear();

        Optional<Quotation> found = repository.findById(quotation.getId());
        assertTrue(found.isPresent());
        assertEquals(vehicle.getId(), found.get().getVehicle().getId());
        assertEquals(customer.getId(), found.get().getCustomer().getId());
    }

    @Test
    public void findById_NotFound() {
        Optional<Quotation> found = repository.findById(UUID.randomUUID());
        assertTrue(found.isEmpty(), "Should be empty, the ID does not exist");
    }

    @Test
    public void findQuotations_WithDynamicFilters_ReturnsCorrectResults() {
        em.getTransaction().begin();
        Vehicle vehicle1 = persistMockVehicle();
        Vehicle vehicle2 = persistMockVehicle();
        Vehicle vehicle3 = persistMockVehicle();
        Customer customer = persistMockCustomer();

        Quotation q1 = new Quotation(vehicle1, customer, TODAY);
        q1.setDate(LocalDate.of(2026, 4, 2));
        q1.updateExpiration(TODAY);
        q1.issue(TODAY);
        q1.accept(TODAY); // ACCEPTED
        repository.save(q1);

        Quotation q2 = new Quotation(vehicle2, customer, TODAY); // DRAFT
        q2.setDate(LocalDate.of(2025, 11, 7));
        q2.archive(TODAY); // archived
        repository.save(q2);

        Quotation q3 = new Quotation(vehicle3, customer, TODAY);
        q3.setDate(LocalDate.of(2026, 3, 12));
        q3.updateExpiration(TODAY);
        q3.issue(TODAY); // ISSUED
        repository.save(q3);

        em.getTransaction().commit();
        em.clear();

        List<Quotation> byDateFrom = repository.findQuotations(LocalDate.of(2026, 1, 1), null, null, null, null, null,
                TODAY);
        assertEquals(2, byDateFrom.size(), "Should find 2 quotations dated 2026");
        assertEquals(LocalDate.of(2026, 4, 2), byDateFrom.get(0).getDate());
        assertEquals(LocalDate.of(2026, 3, 12), byDateFrom.get(1).getDate());

        List<Quotation> byDateRange = repository.findQuotations(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 20),
                null, null, null, null, TODAY);
        assertEquals(1, byDateRange.size(), "Should find 1 quotation");
        assertEquals(LocalDate.of(2026, 3, 12), byDateRange.get(0).getDate());

        List<Quotation> archived = repository.findQuotations(null, null, true, null, null, null, TODAY);
        assertEquals(1, archived.size(), "Should find 1 ARCHIVED quotation");
        assertTrue(archived.get(0).isArchived());
        assertEquals(LocalDate.of(2025, 11, 7), archived.get(0).getDate());

        List<Quotation> byVehicleId = repository.findQuotations(null, null, null, vehicle1.getId(), null, null, TODAY);
        assertEquals(1, byVehicleId.size(), "Should find 1 quotation");
        assertEquals(LocalDate.of(2026, 4, 2), byVehicleId.get(0).getDate());

        List<Quotation> byCustomerId = repository.findQuotations(null, null, null, null, customer.getId(), null, TODAY);
        assertEquals(3, byCustomerId.size(), "Should find 3 quotations");

        List<QuotationStatus> statusList = Arrays.asList(QuotationStatus.ISSUED, QuotationStatus.ACCEPTED);

        List<Quotation> byStatus = repository.findQuotations(null, null, null,
                null, null, statusList, TODAY);
        assertEquals(2, byStatus.size(), "Should find 2 quotations matching the status");
        assertEquals(LocalDate.of(2026, 4, 2), byStatus.get(0).getDate());
        assertEquals(LocalDate.of(2026, 3, 12), byStatus.get(1).getDate());
    }

    @Test
    public void findQuotations_WithEffectiveStatusFilters_ReturnsCorrectResults() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();

        Quotation q1 = new Quotation(vehicle, customer, TODAY);
        q1.setDate(LocalDate.of(2026, 4, 2));
        q1.updateExpiration(TODAY);
        q1.issue(TODAY); // ISSUED
        repository.save(q1);

        Quotation q2 = new Quotation(vehicle, customer, TODAY);
        q2.setDate(LocalDate.of(2026, 3, 12));
        q2.updateExpiration(TODAY.minusDays(1));
        q2.issue(TODAY.minusDays(1)); // ISSUED expired
        repository.save(q2);

        Quotation q3 = new Quotation(vehicle, customer, TODAY);
        q3.setDate(LocalDate.of(2026, 2, 20));
        q3.updateExpiration(TODAY);
        q3.issue(TODAY);
        q3.expire(); // EXPIRED
        repository.save(q3);

        Quotation q4 = new Quotation(vehicle, customer, TODAY); // DRAFT
        q4.setDate(LocalDate.of(2026, 1, 15));
        repository.save(q4);

        em.getTransaction().commit();
        em.clear();

        List<Quotation> issued = repository.findQuotations(null, null, null, null, null,
                List.of(QuotationStatus.ISSUED), TODAY);
        assertEquals(1, issued.size(), "Should find only the ISSUED quotation that is still valid");
        assertEquals(LocalDate.of(2026, 4, 2), issued.get(0).getDate());

        List<Quotation> expired = repository.findQuotations(null, null, null, null, null,
                List.of(QuotationStatus.EXPIRED), TODAY);
        assertEquals(2, expired.size(),
                "Should find the EXPIRED quotation and the ISSUED one past its expiration date");
        assertEquals(LocalDate.of(2026, 3, 12), expired.get(0).getDate());
        assertEquals(LocalDate.of(2026, 2, 20), expired.get(1).getDate());

        List<Quotation> issuedOrDraft = repository.findQuotations(null, null, null, null, null,
                List.of(QuotationStatus.ISSUED, QuotationStatus.DRAFT), TODAY);
        assertEquals(2, issuedOrDraft.size(), "Should find the valid ISSUED quotation and the DRAFT one");
        assertEquals(LocalDate.of(2026, 4, 2), issuedOrDraft.get(0).getDate());
        assertEquals(LocalDate.of(2026, 1, 15), issuedOrDraft.get(1).getDate());

        List<Quotation> issuedOrExpired = repository.findQuotations(null, null, null, null, null,
                List.of(QuotationStatus.ISSUED, QuotationStatus.EXPIRED), TODAY);
        assertEquals(3, issuedOrExpired.size(), "Should find each quotation only once");
        assertEquals(LocalDate.of(2026, 4, 2), issuedOrExpired.get(0).getDate());
        assertEquals(LocalDate.of(2026, 3, 12), issuedOrExpired.get(1).getDate());
        assertEquals(LocalDate.of(2026, 2, 20), issuedOrExpired.get(2).getDate());

        List<Quotation> all = repository.findQuotations(null, null, null, null, null, null, TODAY);
        assertEquals(4, all.size(), "Without a status filter should find all quotations");
    }

    @Test
    public void findVisibleQuotationsByCustomerId() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer1 = persistMockCustomer();
        Customer customer2 = persistMockOtherCustomer();

        Quotation q1 = new Quotation(vehicle, customer1, TODAY);
        q1.setDate(LocalDate.of(2026, 4, 2));
        q1.updateExpiration(TODAY);
        q1.issue(TODAY);
        q1.accept(TODAY); // ACCEPTED
        repository.save(q1);

        Quotation q2 = new Quotation(vehicle, customer2, TODAY); // DRAFT
        q2.setDate(LocalDate.of(2025, 11, 7));
        repository.save(q2);

        Quotation q3 = new Quotation(vehicle, customer2, TODAY);
        q3.setDate(LocalDate.of(2026, 3, 12));
        q3.updateExpiration(TODAY);
        q3.issue(TODAY); // ISSUED
        repository.save(q3);

        em.getTransaction().commit();
        em.clear();

        List<Quotation> visibleToCustomer1 = repository.findVisibleQuotationsByCustomerId(customer1.getId());
        assertEquals(1, visibleToCustomer1.size(), "Should find 1 quotation visible to the first customer");
        assertEquals(LocalDate.of(2026, 4, 2), visibleToCustomer1.get(0).getDate());

        List<Quotation> visibleToCustomer2 = repository.findVisibleQuotationsByCustomerId(customer2.getId());
        assertEquals(1, visibleToCustomer2.size(), "Should find 1 quotation visible to the second customer");
        assertEquals(LocalDate.of(2026, 3, 12), visibleToCustomer2.get(0).getDate());
    }

    @Test
    public void findExpiredQuotations() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();

        Quotation q1 = new Quotation(vehicle, customer, TODAY);
        q1.setDate(LocalDate.of(2026, 1, 5));
        q1.updateExpiration(TODAY.minusDays(1));
        q1.issue(TODAY.minusDays(1)); // ISSUED expired
        repository.save(q1);

        Quotation q2 = new Quotation(vehicle, customer, TODAY);
        q2.setDate(LocalDate.of(2026, 1, 7));
        q2.updateExpiration(TODAY);
        q2.issue(TODAY); // ISSUED
        repository.save(q2);

        em.getTransaction().commit();
        em.clear();

        List<Quotation> expiredQuotation = repository.findExpiredQuotations(TODAY);
        assertEquals(1, expiredQuotation.size(), "Should find 1 ISSUED quotation with expiration date in the past");
        assertEquals(LocalDate.of(2026, 1, 5), expiredQuotation.get(0).getDate());
    }

    @Test
    public void findOpenQuotationsByVehicle() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer1 = persistMockCustomer();
        Customer customer2 = persistMockOtherCustomer();

        Quotation q1 = new Quotation(vehicle, customer1, TODAY);
        q1.setDate(LocalDate.of(2026, 4, 2));
        q1.updateExpiration(TODAY);
        q1.issue(TODAY); // ISSUED
        repository.save(q1);

        Quotation q2 = new Quotation(vehicle, customer1, TODAY); // DRAFT
        q2.setDate(LocalDate.of(2025, 11, 7));
        q2.archive(TODAY); // archived
        repository.save(q2);

        Quotation q3 = new Quotation(vehicle, customer2, TODAY);
        q3.setDate(LocalDate.of(2026, 3, 12));
        q3.updateExpiration(TODAY);
        q3.issue(TODAY); // ISSUED
        repository.save(q3);

        em.getTransaction().commit();
        em.clear();

        List<Quotation> openQuotations = repository.findOpenQuotationsByVehicle(vehicle.getId(), q1.getId());
        assertEquals(2, openQuotations.size(), "Should find 2 open (draft and issued) quotations for the same vehicle");
    }

    @Test
    public void existsIssuedQuotationForVehicle_True() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer1 = persistMockCustomer();
        Customer customer2 = persistMockOtherCustomer();

        Quotation q1 = new Quotation(vehicle, customer1, TODAY);
        q1.setDate(LocalDate.of(2026, 4, 2));
        q1.updateExpiration(TODAY);
        q1.issue(TODAY); // ISSUED
        repository.save(q1);

        Quotation q2 = new Quotation(vehicle, customer1, TODAY); // DRAFT
        q2.setDate(TODAY);
        q2.archive(TODAY); // archived
        repository.save(q2);

        Quotation q3 = new Quotation(vehicle, customer2, TODAY);
        q3.setDate(LocalDate.of(2026, 3, 12));
        q3.updateExpiration(TODAY);
        q3.issue(TODAY); // ISSUED
        repository.save(q3);

        em.getTransaction().commit();
        em.clear();

        boolean existsIssuedQuotation = repository.existsIssuedQuotationForVehicle(vehicle.getId());
        assertTrue(existsIssuedQuotation);
    }

    @Test
    public void existsIssuedQuotationForVehicle_False() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer1 = persistMockCustomer();
        Customer customer2 = persistMockOtherCustomer();

        Quotation q1 = new Quotation(vehicle, customer1, TODAY);
        q1.setDate(LocalDate.of(2026, 4, 2));
        q1.updateExpiration(TODAY);
        q1.issue(TODAY);
        q1.accept(TODAY); // ACCEPTED
        repository.save(q1);

        Quotation q2 = new Quotation(vehicle, customer2, TODAY); // DRAFT
        repository.save(q2);

        Quotation q3 = new Quotation(vehicle, customer1, TODAY); // DRAFT
        q3.setDate(LocalDate.of(2025, 11, 7));
        q3.archive(TODAY); // archived
        repository.save(q3);

        Quotation q4 = new Quotation(vehicle, customer2, TODAY);
        q4.setDate(LocalDate.of(2026, 4, 2));
        q4.updateExpiration(TODAY);
        q4.issue(TODAY); // ISSUED
        q4.expire(); // EXPIRED
        repository.save(q4);
        em.getTransaction().commit();
        em.clear();

        boolean existsIssuedQuotation = repository.existsIssuedQuotationForVehicle(vehicle.getId());
        assertFalse(existsIssuedQuotation);
    }

    @Test
    public void saveQuotation_UpdateMerge_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Quotation quotation = new Quotation(vehicle, customer, TODAY);
        repository.save(quotation);
        em.getTransaction().commit();

        UUID quotationId = quotation.getId();

        em.clear();
        em.getTransaction().begin();

        Quotation quotationToUpdate = repository.findById(quotationId).get();
        quotationToUpdate.setDate(LocalDate.of(2025, 10, 30));

        repository.save(quotationToUpdate); // em.merge()
        em.getTransaction().commit();
        em.clear();

        Quotation updatedQuotation = repository.findById(quotationId).get();
        assertEquals(LocalDate.of(2025, 10, 30), updatedQuotation.getDate(), "Date should be updated");
    }

    @Test
    public void cloneQuotation_PersistsDiscount() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Quotation original = new Quotation(vehicle, customer, TODAY);
        DiscountStrategy percentageDiscountStrategy = new PercentageDiscountStrategy(new BigDecimal("10"));
        original.setDiscountStrategy(percentageDiscountStrategy);

        Quotation quotation = new Quotation(original, TODAY);
        repository.save(quotation);
        em.getTransaction().commit();

        UUID quotationId = quotation.getId();

        em.clear();

        Quotation clonedQuotation = repository.findById(quotationId)
                .orElseThrow(() -> new AssertionError("Quotation not persisted"));
        assertEquals(DiscountType.PERCENTAGE, clonedQuotation.getDiscountType());
        assertEquals(0, clonedQuotation.getDiscountValue().compareTo(new BigDecimal("10")));
    }

    @Test
    public void deleteQuotation_AttachedEntity_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Quotation quotation = new Quotation(vehicle, customer, TODAY);
        repository.save(quotation);

        repository.delete(quotation); // em.remove(quotation)
        em.getTransaction().commit();
        em.clear();

        Optional<Quotation> deletedQuotation = repository.findById(quotation.getId());
        assertTrue(deletedQuotation.isEmpty(), "Should be empty, the quotation has been eliminated");
    }

    @Test
    public void deleteQuotation_DetachedEntity_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Quotation quotation = new Quotation(vehicle, customer, TODAY);

        repository.save(quotation);
        em.getTransaction().commit();

        UUID quotationId = quotation.getId();
        assertNotNull(quotationId);

        em.clear();
        em.getTransaction().begin();

        Quotation quotationToDelete = repository.findById(quotationId)
                .orElseThrow(() -> new IllegalStateException("Quotation not found"));

        em.clear();
        repository.delete(quotationToDelete); // em.remove(em.merge(quotation))

        em.getTransaction().commit();
        em.clear();

        Optional<Quotation> deletedQuotation = repository.findById(quotationId);
        assertTrue(deletedQuotation.isEmpty(), "Should be empty, the quotation has been eliminated");
    }

    // helper methods
    private Vehicle persistMockVehicle() {
        Vehicle v = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setCondition(VehicleCondition.NEW)
                .setSellingPrice(new BigDecimal("20000"))
                .setIsInShowroom(true)
                .build();
        em.persist(v);
        return v;
    }

    private Customer persistMockCustomer() {
        Customer c = new Customer.CustomerBuilder()
                .setFirstName("Mario")
                .setLastName("Rossi")
                .setEmail("mario@test.com")
                .setPhoneNumber("12345")
                .setResidenceCity("Roma")
                .build();
        em.persist(c);
        return c;
    }

    private Customer persistMockOtherCustomer() {
        Customer c = new Customer.CustomerBuilder()
                .setFirstName("Sofia")
                .setLastName("Verdi")
                .setEmail("sofia@test.com")
                .setPhoneNumber("67890")
                .setResidenceCity("Palermo")
                .build();
        em.persist(c);
        return c;
    }
}