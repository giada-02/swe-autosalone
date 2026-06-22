package com.autosalone.repositories;

import com.autosalone.enums.SortOrder;
import com.autosalone.enums.TransactionType;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.models.Contract;
import com.autosalone.models.Customer;
import com.autosalone.models.Transaction;
import com.autosalone.models.TransactionFactory;
import com.autosalone.models.Vehicle;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionRepositoryTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private TransactionRepository repository;

    @BeforeEach
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("autosalonePU-test");
        em = emf.createEntityManager();
        repository = new TransactionRepository();
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
    public void saveTransaction_Success() {
        em.getTransaction().begin();
        Transaction transaction = TransactionFactory.createGeneralIncome("Vendita", new BigDecimal("500.00"),
                LocalDate.now());

        repository.save(transaction);
        em.getTransaction().commit();
        assertNotNull(transaction.getId());

        em.clear();

        Optional<Transaction> found = repository.findById(transaction.getId());
        assertTrue(found.isPresent());
        assertEquals("Vendita", found.get().getReason());
    }

    @Test
    public void findById_NotFound() {
        Optional<Transaction> found = repository.findById(UUID.randomUUID());
        assertTrue(found.isEmpty(), "Should be empty, the ID does not exist");
    }

    @Test
    public void findTransactions_WithDynamicFilters_ReturnsCorrectResults() {
        em.getTransaction().begin();

        Transaction t1 = TransactionFactory.createGeneralIncome("Vendita 1", new BigDecimal("5000.00"),
                LocalDate.of(2023, 1, 10));
        repository.save(t1);

        Transaction t2 = TransactionFactory.createGeneralExpense("Spesa 1", new BigDecimal("200.00"),
                LocalDate.of(2023, 1, 15));
        repository.save(t2);

        Transaction t3 = TransactionFactory.createGeneralIncome("Vendita 2", new BigDecimal("3000.00"),
                LocalDate.of(2023, 1, 20));
        repository.save(t3);

        em.getTransaction().commit();
        em.clear();

        List<Transaction> filtered = repository.findTransactions(
                LocalDate.of(2023, 1, 10),
                LocalDate.of(2023, 1, 18),
                TransactionType.IN,
                SortOrder.ASC);
        assertEquals(1, filtered.size());
        assertEquals("Vendita 1", filtered.get(0).getReason());

        List<Transaction> allDesc = repository.findTransactions(null, null, null, SortOrder.DESC);
        assertEquals(3, allDesc.size());
        assertEquals("Vendita 2", allDesc.get(0).getReason());
    }

    @Test
    public void findAllExpenses_ByVehicle() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();

        Transaction expense = TransactionFactory.createVehicleExpense(vehicle, "Cambio gomme",
                new BigDecimal("300.00"), LocalDate.now());
        repository.save(expense);

        Transaction unrelated = TransactionFactory.createGeneralExpense("Spesa generale", new BigDecimal("50.00"),
                LocalDate.now());
        repository.save(unrelated);

        em.getTransaction().commit();
        em.clear();

        List<Transaction> expenses = repository.findAllExpenses(vehicle.getId());
        assertEquals(1, expenses.size());
        assertTrue(expenses.get(0).getReason().contains("Cambio gomme"));
    }

    @Test
    public void findAllPayments_ByContract() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle();
        Customer customer = persistMockCustomer();
        Contract contract = new Contract(vehicle, customer);
        em.persist(contract);

        Transaction payment = TransactionFactory.createContractPayment(contract, "Bonifico", new BigDecimal("500.00"),
                LocalDate.now());
        repository.save(payment);

        Transaction unrelated = TransactionFactory.createGeneralIncome("Vendita", new BigDecimal("50.00"),
                LocalDate.now());
        repository.save(unrelated);

        em.getTransaction().commit();
        em.clear();

        List<Transaction> payments = repository.findAllPayments(contract.getId());
        assertEquals(1, payments.size());
        assertTrue(payments.get(0).getReason().contains("Bonifico"));
    }

    @Test
    public void sumTransactions_CalculatesCorrectly() {
        em.getTransaction().begin();
        repository.save(TransactionFactory.createGeneralIncome("Acconto", new BigDecimal("1000.00"),
                LocalDate.of(2023, 5, 10)));
        repository.save(
                TransactionFactory.createGeneralIncome("Saldo", new BigDecimal("2000.00"), LocalDate.of(2023, 5, 12)));
        repository.save(TransactionFactory.createGeneralExpense("Riparazione", new BigDecimal("500.00"),
                LocalDate.of(2023, 5, 15)));
        em.getTransaction().commit();
        em.clear();

        BigDecimal sumIn = repository.sumInTransactions(LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 31));
        assertEquals(new BigDecimal("3000.00"), sumIn);

        BigDecimal sumOut = repository.sumOutTransactions(null, null);
        assertEquals(new BigDecimal("500.00"), sumOut);

        BigDecimal sumZero = repository.sumInTransactions(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        assertEquals(BigDecimal.ZERO, sumZero);
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
}