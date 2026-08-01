package com.autosalone.repositories;

import com.autosalone.models.Customer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerRepositoryTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private CustomerRepository repository;

    @BeforeEach
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("autosalonePU-test");
        em = emf.createEntityManager();

        repository = new CustomerRepository();
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
    public void saveCustomer_Success() {
        em.getTransaction().begin();
        Customer customer = new Customer.CustomerBuilder()
                .setFirstName("Mario")
                .setLastName("Rossi")
                .setEmail("mario@example.com")
                .setPhoneNumber("123456789")
                .setResidenceCity("Firenze")
                .build();

        repository.save(customer);
        em.getTransaction().commit();
        assertNotNull(customer.getId());

        em.clear(); // clear Hibernate cache to query the database

        Optional<Customer> found = repository.findById(customer.getId());
        assertTrue(found.isPresent());
        assertEquals("Mario", found.get().getFirstName()); // from users table
        assertEquals("Firenze", found.get().getResidenceCity()); // from customers table
    }

    @Test
    public void findById_NotFound() {
        Optional<Customer> found = repository.findById(UUID.randomUUID());
        assertTrue(found.isEmpty(), "Should be empty, the ID does not exist");
    }

    @Test
    public void findCustomers_WithDynamicFilters_ReturnsCorrectResults() {
        em.getTransaction().begin();

        Customer c1 = new Customer.CustomerBuilder()
                .setFirstName("Mario")
                .setLastName("Rossi")
                .setEmail("mario@example.com")
                .setPhoneNumber("111222")
                .setResidenceCity("Arezzo")
                .build();
        c1.activate();
        repository.save(c1);

        Customer c2 = new Customer.CustomerBuilder()
                .setFirstName("Giulia")
                .setLastName("Rossi")
                .setEmail("giulia.r@example.com")
                .setPhoneNumber("333444")
                .setResidenceCity("Pisa")
                .build();
        repository.save(c2);

        Customer c3 = new Customer.CustomerBuilder()
                .setFirstName("Luigi")
                .setLastName("Verdi")
                .setEmail("luigi@example.com")
                .setPhoneNumber("555666")
                .setResidenceCity("Firenze")
                .build();
        c3.activate();
        repository.save(c3);

        em.getTransaction().commit();
        em.clear();

        List<Customer> onlyKeyword = repository.findCustomers("rosSI", null); // case-insensitive
        assertEquals(2, onlyKeyword.size(), "Should find 2 customers with 'rossi' as first_name or last_name");

        List<Customer> onlyActive = repository.findCustomers(null, true);
        assertEquals(2, onlyActive.size(), "Should find 2 active customers");

        List<Customer> combined = repository.findCustomers("ros", true);
        assertEquals(1, combined.size(), "Should find only 1 active customer with 'rossi' as last_name");
        assertEquals("Mario", combined.get(0).getFirstName());

        List<Customer> noFilters = repository.findCustomers(null, null);
        assertEquals(3, noFilters.size(), "Without filters every customer should be found");
    }

    @Test
    public void saveCustomer_UpdateMerge_Success() {
        em.getTransaction().begin();
        Customer customer = new Customer.CustomerBuilder()
                .setFirstName("Luca")
                .setLastName("Gialli")
                .setEmail("luca@example.com")
                .setPhoneNumber("123123")
                .setResidenceCity("Roma")
                .build();
        repository.save(customer);
        em.getTransaction().commit();

        UUID customerId = customer.getId();

        em.clear();
        em.getTransaction().begin();

        Customer customerToUpdate = repository.findById(customerId).get();
        customerToUpdate.setPhoneNumber("999999");

        repository.save(customerToUpdate); // em.merge()
        em.getTransaction().commit();
        em.clear();

        Customer updatedCustomer = repository.findById(customerId).get();
        assertEquals("999999", updatedCustomer.getPhoneNumber(), "Phone number should be updated");
    }

    @Test
    public void deleteCustomer_AttachedEntity_Success() {
        em.getTransaction().begin();
        Customer customer = new Customer.CustomerBuilder()
                .setFirstName("Andrea")
                .setLastName("Bianchi")
                .setEmail("andrea.bianchi@example.com")
                .setPhoneNumber("333111222")
                .build();
        repository.save(customer);

        repository.delete(customer); // em.remove(customer)
        em.getTransaction().commit();
        em.clear();

        Optional<Customer> deletedCustomer = repository.findById(customer.getId());
        assertTrue(deletedCustomer.isEmpty(), "Should be empty, the customer has been eliminated");
    }

    @Test
    public void deleteCustomer_DetachedEntity_Success() {
        em.getTransaction().begin();
        Customer customer = new Customer.CustomerBuilder()
                .setFirstName("Chiara")
                .setLastName("Bianchi")
                .setEmail("chiara.bianchi@example.com")
                .setPhoneNumber("333111222")
                .setResidenceCity("Lucca")
                .build();

        repository.save(customer);
        em.getTransaction().commit();

        UUID customerId = customer.getId();
        assertNotNull(customerId);

        em.clear();
        em.getTransaction().begin();

        Customer customerToDelete = repository.findById(customerId)
                .orElseThrow(() -> new IllegalStateException("Customer not found"));

        em.clear();
        repository.delete(customerToDelete); // em.remove(em.merge(customer))

        em.getTransaction().commit();
        em.clear();

        Optional<Customer> deletedCustomer = repository.findById(customerId);
        assertTrue(deletedCustomer.isEmpty(), "Should be empty, the customer has been eliminated");
    }
}