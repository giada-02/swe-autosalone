package com.autosalone.repositories;

import com.autosalone.enums.ContractStatus;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.models.Customer;
import com.autosalone.models.Contract;
import com.autosalone.models.Vehicle;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ContractRepositoryTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private ContractRepository repository;

    @BeforeEach
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("autosalonePU-test");
        em = emf.createEntityManager();
        repository = new ContractRepository();
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
    public void saveContract_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Contract contract = new Contract(vehicle, customer);

        repository.save(contract);
        em.getTransaction().commit();
        assertNotNull(contract.getId());

        em.clear();

        Optional<Contract> found = repository.findById(contract.getId());
        assertTrue(found.isPresent());
        assertEquals(vehicle.getId(), found.get().getVehicle().getId());
        assertEquals(customer.getId(), found.get().getCustomer().getId());
    }

    @Test
    public void findById_NotFound() {
        Optional<Contract> found = repository.findById(UUID.randomUUID());
        assertTrue(found.isEmpty(), "Should be empty, the ID does not exist");
    }

    @Test
    public void findContracts_WithDynamicFilters_ReturnsCorrectResults() {
        em.getTransaction().begin();
        Vehicle vehicle1 = persistMockVehicle();
        Vehicle vehicle2 = persistMockVehicle();
        Vehicle vehicle3 = persistMockVehicle();
        Customer customer = persistMockCustomer();

        Contract c1 = new Contract(vehicle1, customer);
        c1.setDate(LocalDate.of(2026, 4, 2));
        c1.setEstimatedHandoverDate(LocalDate.now());
        c1.confirm(null); // CONFIRMED
        repository.save(c1);

        Contract c2 = new Contract(vehicle2, customer); // DRAFT
        c2.setDate(LocalDate.of(2025, 11, 7));
        c2.archive(); // archived
        repository.save(c2);

        Contract c3 = new Contract(vehicle3, customer);
        c3.setDate(LocalDate.of(2026, 3, 12));
        c3.setEstimatedHandoverDate(LocalDate.now());
        c3.confirm(null);
        c3.cancel("Ripensamento"); // CANCELED
        repository.save(c3);

        em.getTransaction().commit();
        em.clear();

        List<Contract> byDateFrom = repository.findContracts(LocalDate.of(2026, 1, 1), null, null, null, null, null);
        assertEquals(2, byDateFrom.size(), "Should find 2 contracts dated 2026");
        assertEquals(LocalDate.of(2026, 4, 2), byDateFrom.get(0).getDate());
        assertEquals(LocalDate.of(2026, 3, 12), byDateFrom.get(1).getDate());

        List<Contract> byDateRange = repository.findContracts(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 20),
                null, null, null, null);
        assertEquals(1, byDateRange.size(), "Should find 1 contract");
        assertEquals(LocalDate.of(2026, 3, 12), byDateRange.get(0).getDate());

        List<Contract> archived = repository.findContracts(null, null, true, null, null, null);
        assertEquals(1, archived.size(), "Should find 1 ARCHIVED contract");
        assertTrue(archived.get(0).isArchived());
        assertEquals(LocalDate.of(2025, 11, 7), archived.get(0).getDate());

        List<Contract> byVehicleId = repository.findContracts(null, null, null, vehicle1.getId(), null, null);
        assertEquals(1, byVehicleId.size(), "Should find 1 contract");
        assertEquals(LocalDate.of(2026, 4, 2), byVehicleId.get(0).getDate());

        List<Contract> byCustomerId = repository.findContracts(null, null, null, null, customer.getId(), null);
        assertEquals(3, byCustomerId.size(), "Should find 3 contracts");

        List<ContractStatus> statuses = Arrays.asList(ContractStatus.CONFIRMED, ContractStatus.CANCELED);

        List<Contract> byStatus = repository.findContracts(null, null, null,
                null, null, statuses);
        assertEquals(2, byStatus.size(), "Should find 2 contracts matching the status");
        assertEquals(LocalDate.of(2026, 4, 2), byDateFrom.get(0).getDate());
        assertEquals(LocalDate.of(2026, 3, 12), byDateFrom.get(1).getDate());
    }

    @Test
    public void findVisibleContractsByCustomerId() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer1 = persistMockCustomer();
        Customer customer2 = persistMockOtherCustomer();

        Contract c1 = new Contract(vehicle, customer1);
        c1.setDate(LocalDate.of(2026, 4, 2));
        c1.setEstimatedHandoverDate(LocalDate.now());
        c1.confirm(null); // CONFIRMED
        repository.save(c1);

        Contract c2 = new Contract(vehicle, customer2); // DRAFT
        c2.setDate(LocalDate.of(2025, 11, 7));
        repository.save(c2);

        Contract c3 = new Contract(vehicle, customer2);
        c3.setDate(LocalDate.of(2026, 3, 12));
        c3.setEstimatedHandoverDate(LocalDate.now());
        c3.confirm(null);
        c3.cancel("Ripensamento"); // CANCELED
        repository.save(c3);

        em.getTransaction().commit();
        em.clear();

        List<Contract> visibleToCustomer1 = repository.findVisibleContractsByCustomerId(customer1.getId());
        assertEquals(1, visibleToCustomer1.size(), "Should find 1 contract visible to the first customer");
        assertEquals(LocalDate.of(2026, 4, 2), visibleToCustomer1.get(0).getDate());

        List<Contract> visibleToCustomer2 = repository.findVisibleContractsByCustomerId(customer2.getId());
        assertEquals(1, visibleToCustomer2.size(), "Should find 1 contract visible to the second customer");
        assertEquals(LocalDate.of(2026, 3, 12), visibleToCustomer2.get(0).getDate());
    }

    @Test
    public void findConflictingContractsForVehicle() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer1 = persistMockCustomer();
        Customer customer2 = persistMockOtherCustomer();

        Contract c1 = new Contract(vehicle, customer1);
        c1.setDate(LocalDate.of(2026, 4, 2));
        c1.setEstimatedHandoverDate(LocalDate.now());
        c1.confirm(null); // CONFIRMED
        repository.save(c1);

        Contract c2 = new Contract(vehicle, customer1); // DRAFT
        c2.setDate(LocalDate.of(2025, 11, 7));
        c2.archive(); // archived
        repository.save(c2);

        Contract c3 = new Contract(vehicle, customer2); // DRAFT
        c3.setDate(LocalDate.of(2026, 3, 12));
        repository.save(c3);

        em.getTransaction().commit();
        em.clear();

        List<Contract> conflictingContracts = repository.findConflictingContractsForVehicle(vehicle.getId(),
                c1.getId());
        assertEquals(2, conflictingContracts.size(),
                "Should find 2 conflicting contracts for the same vehicle");
    }

    @Test
    public void saveContract_UpdateMerge_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Contract contract = new Contract(vehicle, customer);
        repository.save(contract);
        em.getTransaction().commit();

        UUID contractId = contract.getId();

        em.clear();
        em.getTransaction().begin();

        Contract contractToUpdate = repository.findById(contractId).get();
        contractToUpdate.setDate(LocalDate.of(2025, 10, 30));

        repository.save(contractToUpdate); // em.merge()
        em.getTransaction().commit();
        em.clear();

        Contract updatedContract = repository.findById(contractId).get();
        assertEquals(LocalDate.of(2025, 10, 30), updatedContract.getDate(), "Date should be updated");
    }

    @Test
    public void deleteContract_AttachedEntity_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Contract contract = new Contract(vehicle, customer);
        repository.save(contract);

        repository.delete(contract); // em.remove(contract)
        em.getTransaction().commit();
        em.clear();

        Optional<Contract> deletedContract = repository.findById(contract.getId());
        assertTrue(deletedContract.isEmpty(), "Should be empty, the contract has been eliminated");
    }

    @Test
    public void deleteContract_DetachedEntity_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Contract contract = new Contract(vehicle, customer);

        repository.save(contract);
        em.getTransaction().commit();

        UUID contractId = contract.getId();
        assertNotNull(contractId);

        em.clear();
        em.getTransaction().begin();

        Contract contractToDelete = repository.findById(contractId)
                .orElseThrow(() -> new IllegalStateException("Contract not found"));

        em.clear();
        repository.delete(contractToDelete); // em.remove(em.merge(contract))

        em.getTransaction().commit();
        em.clear();

        Optional<Contract> deletedContract = repository.findById(contractId);
        assertTrue(deletedContract.isEmpty(), "Should be empty, the contract has been eliminated");
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
                .setZipCode("00000")
                .setFiscalCode("RSSMRA00X00X000X")
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
                .setZipCode("00000")
                .setFiscalCode("VRDSFO00X00X000X")
                .build();
        em.persist(c);
        return c;
    }
}