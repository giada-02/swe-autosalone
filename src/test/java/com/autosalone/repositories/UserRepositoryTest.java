package com.autosalone.repositories;

import com.autosalone.models.Customer;
import com.autosalone.models.Owner;
import com.autosalone.models.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest {

        private EntityManagerFactory emf;
        private EntityManager em;
        private UserRepository repository;

        @BeforeEach
        public void setUp() {
                emf = Persistence.createEntityManagerFactory("autosalonePU-test");
                em = emf.createEntityManager();

                repository = new UserRepository();
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
        public void saveUser_Success() {
                em.getTransaction().begin();

                Owner owner = new Owner.OwnerBuilder()
                                .setFirstName("Sofia")
                                .setLastName("Rossi")
                                .setEmail("sofia.r@example.com")
                                .setPhoneNumber("333444")
                                .build();
                repository.save(owner);

                Customer customer = new Customer.CustomerBuilder()
                                .setFirstName("Mario")
                                .setLastName("Rossi")
                                .setEmail("mario@example.com")
                                .setPhoneNumber("123456789")
                                .setResidenceCity("Firenze")
                                .build();
                repository.save(customer);

                em.getTransaction().commit();
                assertNotNull(owner.getId());
                assertNotNull(customer.getId());

                em.clear();

                Optional<User> foundOwner = repository.findById(owner.getId());
                assertTrue(foundOwner.isPresent());
                assertEquals("Sofia", foundOwner.get().getFirstName());
                Optional<User> foundCustomer = repository.findById(customer.getId());
                assertTrue(foundCustomer.isPresent());
                assertEquals("Mario", foundCustomer.get().getFirstName());
        }

        @Test
        public void findById_NotFound() {
                Optional<User> found = repository.findById(UUID.randomUUID());
                assertTrue(found.isEmpty(), "Should be empty, the ID does not exist");
        }

        @Test
        public void findByEmail_SuccessAndNotFound() {
                em.getTransaction().begin();

                Owner owner = new Owner.OwnerBuilder()
                                .setFirstName("Anna")
                                .setLastName("Neri")
                                .setEmail("anna.neri@example.com")
                                .setPhoneNumber("333444")
                                .build();
                repository.save(owner);

                Customer customer = new Customer.CustomerBuilder()
                                .setFirstName("Mario")
                                .setLastName("Rossi")
                                .setEmail("mario@example.com")
                                .setPhoneNumber("123456789")
                                .setResidenceCity("Firenze")
                                .build();
                repository.save(customer);

                em.getTransaction().commit();
                em.clear();

                // found
                Optional<User> foundOwner = repository.findByEmail("anna.neri@example.com");
                assertTrue(foundOwner.isPresent(), "Should find the user by email");
                assertEquals("Anna", foundOwner.get().getFirstName());
                Optional<User> foundCustomer = repository.findByEmail("mario@example.com");
                assertTrue(foundCustomer.isPresent(), "Should find the user by email");
                assertEquals("Mario", foundCustomer.get().getFirstName());

                // not found
                Optional<User> notFound = repository.findByEmail("non.esiste@example.com");
                assertTrue(notFound.isEmpty(), "Should return empty Optional for non-existent email");
        }

        @Test
        public void saveUser_UpdateMerge_Success() {
                em.getTransaction().begin();
                Owner owner = new Owner.OwnerBuilder()
                                .setFirstName("Alice")
                                .setLastName("Rossi")
                                .setEmail("alice.r@example.com")
                                .setPhoneNumber("333444")
                                .build();
                repository.save(owner);

                Customer customer = new Customer.CustomerBuilder()
                                .setFirstName("Mario")
                                .setLastName("Rossi")
                                .setEmail("mario@example.com")
                                .setPhoneNumber("123456789")
                                .setResidenceCity("Firenze")
                                .build();
                repository.save(customer);
                em.getTransaction().commit();

                UUID ownerId = owner.getId();
                UUID customerId = customer.getId();

                em.clear();
                em.getTransaction().begin();

                User ownerToUpdate = repository.findById(ownerId).get();
                ownerToUpdate.setPhoneNumber("999999");
                User customerToUpdate = repository.findById(customerId).get();
                customerToUpdate.setPhoneNumber("111111");

                repository.save(ownerToUpdate); // em.merge()
                repository.save(customerToUpdate);
                em.getTransaction().commit();
                em.clear();

                User updatedOwner = repository.findById(ownerId).get();
                assertEquals("999999", updatedOwner.getPhoneNumber(), "Phone number should be updated");
                User updatedCustomer = repository.findById(customerId).get();
                assertEquals("111111", updatedCustomer.getPhoneNumber(), "Phone number should be updated");
        }

        @Test
        public void deleteUser_AttachedEntity_Success() {
                em.getTransaction().begin();
                Owner user = new Owner.OwnerBuilder()
                                .setFirstName("Alice")
                                .setLastName("Rossi")
                                .setEmail("alice.r@example.com")
                                .setPhoneNumber("333444")
                                .build();
                repository.save(user);

                repository.delete(user); // em.remove(user)
                em.getTransaction().commit();
                em.clear();

                Optional<User> deletedUser = repository.findById(user.getId());
                assertTrue(deletedUser.isEmpty(), "Should be empty, the user has been eliminated");
        }

        @Test
        public void deleteUser_DetachedEntity_Success() {
                em.getTransaction().begin();
                Customer user = new Customer.CustomerBuilder()
                                .setFirstName("Mario")
                                .setLastName("Rossi")
                                .setEmail("mario@example.com")
                                .setPhoneNumber("123456789")
                                .setResidenceCity("Firenze")
                                .build();

                repository.save(user);
                em.getTransaction().commit();

                UUID userId = user.getId();
                assertNotNull(userId);

                em.clear();
                em.getTransaction().begin();

                User userToDelete = repository.findById(userId)
                                .orElseThrow(() -> new IllegalStateException("User not found"));

                em.clear();
                repository.delete(userToDelete); // em.remove(em.merge(user))

                em.getTransaction().commit();
                em.clear();

                Optional<User> deletedUser = repository.findById(userId);
                assertTrue(deletedUser.isEmpty(), "Should be empty, the user has been eliminated");
        }
}