package com.autosalone.repositories;

import com.autosalone.enums.QuotationStatus;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.models.Customer;
import com.autosalone.models.Quotation;
import com.autosalone.models.Vehicle;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class QuotationRepositoryTest {

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
        Quotation quotation = new Quotation(vehicle, customer);

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

        Quotation q1 = new Quotation(vehicle1, customer);
        q1.setDate(LocalDate.of(2026, 4, 2));
        q1.setExpirationDate(LocalDate.of(9999, 1, 1));
        q1.issue();
        q1.accept(); // ACCEPTED
        repository.save(q1);

        Quotation q2 = new Quotation(vehicle2, customer); // DRAFT
        q2.setDate(LocalDate.of(2025, 11, 7));
        q2.archive(); // archived
        repository.save(q2);

        Quotation q3 = new Quotation(vehicle3, customer);
        q3.setDate(LocalDate.of(2026, 3, 12));
        q3.setExpirationDate(LocalDate.of(9999, 1, 1));
        q3.issue(); // ISSUED
        repository.save(q3);

        em.getTransaction().commit();
        em.clear();

        List<Quotation> byDateFrom = repository.findQuotations(LocalDate.of(2026, 1, 1), null, null, null, null, null);
        assertEquals(2, byDateFrom.size(), "Should find 2 quotations dated 2026");
        assertEquals(LocalDate.of(2026, 4, 2), byDateFrom.get(0).getDate());
        assertEquals(LocalDate.of(2026, 3, 12), byDateFrom.get(1).getDate());

        List<Quotation> byDateRange = repository.findQuotations(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 20),
                null, null, null, null);
        assertEquals(1, byDateRange.size(), "Should find 1 quotation");
        assertEquals(LocalDate.of(2026, 3, 12), byDateRange.get(0).getDate());

        List<Quotation> archived = repository.findQuotations(null, null, true, null, null, null);
        assertEquals(1, archived.size(), "Should find 1 ARCHIVED quotation");
        assertTrue(archived.get(0).isArchived());
        assertEquals(LocalDate.of(2025, 11, 7), archived.get(0).getDate());

        List<Quotation> byVehicleId = repository.findQuotations(null, null, null, vehicle1.getId(), null, null);
        assertEquals(1, byVehicleId.size(), "Should find 1 quotation");
        assertEquals(LocalDate.of(2026, 4, 2), byVehicleId.get(0).getDate());

        List<Quotation> byCustomerId = repository.findQuotations(null, null, null, null, customer.getId(), null);
        assertEquals(3, byCustomerId.size(), "Should find 3 quotations");

        List<QuotationStatus> statuses = Arrays.asList(QuotationStatus.ISSUED, QuotationStatus.ACCEPTED);

        List<Quotation> byStatus = repository.findQuotations(null, null, null,
                null, null, statuses);
        assertEquals(2, byStatus.size(), "Should find 2 quotations matching the status");
        assertEquals(LocalDate.of(2026, 4, 2), byDateFrom.get(0).getDate());
        assertEquals(LocalDate.of(2026, 3, 12), byDateFrom.get(1).getDate());
    }

    @Test
    public void findVisibleQuotationsByCustomerId() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer1 = persistMockCustomer();
        Customer customer2 = persistMockOtherCustomer();

        Quotation q1 = new Quotation(vehicle, customer1);
        q1.setDate(LocalDate.of(2026, 4, 2));
        q1.setExpirationDate(LocalDate.of(9999, 1, 1));
        q1.issue();
        q1.accept(); // ACCEPTED
        repository.save(q1);

        Quotation q2 = new Quotation(vehicle, customer2); // DRAFT
        q2.setDate(LocalDate.of(2025, 11, 7));
        repository.save(q2);

        Quotation q3 = new Quotation(vehicle, customer2);
        q3.setDate(LocalDate.of(2026, 3, 12));
        q3.setExpirationDate(LocalDate.of(9999, 1, 1));
        q3.issue(); // ISSUED
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

        Quotation q1 = new Quotation(vehicle, customer);
        q1.setDate(LocalDate.of(2026, 1, 5));
        q1.setExpirationDate(LocalDate.now());
        q1.issue(); // ISSUED
        setPastExpirationDateForTesting(q1, 1); // expired
        repository.save(q1);

        Quotation q2 = new Quotation(vehicle, customer);
        q2.setDate(LocalDate.of(2026, 1, 7));
        q2.setExpirationDate(LocalDate.now());
        q2.issue(); // ISSUED
        repository.save(q2);

        em.getTransaction().commit();
        em.clear();

        List<Quotation> expiredQuotation = repository.findExpiredQuotations(LocalDate.now());
        assertEquals(1, expiredQuotation.size(), "Should find 1 ISSUED quotation with expiration date in the past");
        assertEquals(LocalDate.of(2026, 1, 5), expiredQuotation.get(0).getDate());
    }

    @Test
    public void findConflictingQuotationsForVehicle() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer1 = persistMockCustomer();
        Customer customer2 = persistMockOtherCustomer();

        Quotation q1 = new Quotation(vehicle, customer1);
        q1.setDate(LocalDate.of(2026, 4, 2));
        q1.setExpirationDate(LocalDate.of(9999, 1, 1));
        q1.issue(); // ISSUED
        repository.save(q1);

        Quotation q2 = new Quotation(vehicle, customer1); // DRAFT
        q2.setDate(LocalDate.of(2025, 11, 7));
        q2.archive(); // archived
        repository.save(q2);

        Quotation q3 = new Quotation(vehicle, customer2);
        q3.setDate(LocalDate.of(2026, 3, 12));
        q3.setExpirationDate(LocalDate.of(9999, 1, 1));
        q3.issue(); // ISSUED
        repository.save(q3);

        em.getTransaction().commit();
        em.clear();

        List<Quotation> conflictingQuotations = repository.findConflictingQuotationsForVehicle(vehicle.getId(),
                q1.getId());
        assertEquals(2, conflictingQuotations.size(),
                "Should find 2 conflicting quotations for the same vehicle");
    }

    @Test
    public void saveQuotation_UpdateMerge_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Quotation quotation = new Quotation(vehicle, customer);
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
    public void deleteQuotation_AttachedEntity_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Quotation quotation = new Quotation(vehicle, customer);
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
        Quotation quotation = new Quotation(vehicle, customer);

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