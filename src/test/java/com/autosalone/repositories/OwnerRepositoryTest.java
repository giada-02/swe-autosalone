package com.autosalone.repositories;

import com.autosalone.models.Owner;
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

public class OwnerRepositoryTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private OwnerRepository repository;

    @BeforeEach
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("autosalonePU-test");
        em = emf.createEntityManager();

        repository = new OwnerRepository();
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
    public void saveOwner_Success() {
        em.getTransaction().begin();
        Owner owner = new Owner.OwnerBuilder()
                .setFirstName("Mario")
                .setLastName("Rossi")
                .setEmail("mario@example.com")
                .setPhoneNumber("123456789")
                .build();

        repository.save(owner);
        em.getTransaction().commit();
        assertNotNull(owner.getId());

        em.clear();

        Optional<Owner> found = repository.findById(owner.getId());
        assertTrue(found.isPresent());
        assertEquals("Mario", found.get().getFirstName()); // from users table
    }

    @Test
    public void findById_NotFound() {
        Optional<Owner> found = repository.findById(UUID.randomUUID());
        assertTrue(found.isEmpty(), "Should be empty, the ID does not exist");
    }

    @Test
    public void findByEmail_SuccessAndNotFound() {
        em.getTransaction().begin();
        Owner owner = new Owner.OwnerBuilder()
                .setFirstName("Anna")
                .setLastName("Neri")
                .setEmail("anna.neri@example.com")
                .setPhoneNumber("999888")
                .build();
        repository.save(owner);

        em.getTransaction().commit();
        em.clear();

        // found
        Optional<Owner> found = repository.findByEmail("anna.neri@example.com");
        assertTrue(found.isPresent(), "Should find the owner by email");
        assertEquals("Anna", found.get().getFirstName());

        // not found
        Optional<Owner> notFound = repository.findByEmail("non.esiste@example.com");
        assertTrue(notFound.isEmpty(), "Should return empty Optional for non-existent email");
    }

    @Test
    public void findOwners() {
        em.getTransaction().begin();

        Owner o1 = new Owner.OwnerBuilder()
                .setFirstName("Mario")
                .setLastName("Rossi")
                .setEmail("mario@example.com")
                .setPhoneNumber("111222")
                .build();
        repository.save(o1);

        Owner o2 = new Owner.OwnerBuilder()
                .setFirstName("Sofia")
                .setLastName("Rossi")
                .setEmail("sofia.r@example.com")
                .setPhoneNumber("333444")
                .build();
        repository.save(o2);

        em.getTransaction().commit();
        em.clear();

        List<Owner> owners = repository.findOwners(null);
        assertEquals(2, owners.size(), "Should find the 2 owners");
        assertEquals("Mario", owners.get(0).getFirstName());
        assertEquals("Sofia", owners.get(1).getFirstName());
    }

    @Test
    public void saveOwner_UpdateMerge_Success() {
        em.getTransaction().begin();
        Owner owner = new Owner.OwnerBuilder()
                .setFirstName("Luca")
                .setLastName("Gialli")
                .setEmail("luca@example.com")
                .setPhoneNumber("123123")
                .build();
        repository.save(owner);
        em.getTransaction().commit();

        UUID ownerId = owner.getId();

        em.clear();
        em.getTransaction().begin();

        Owner ownerToUpdate = repository.findById(ownerId).get();
        ownerToUpdate.setPhoneNumber("999999");

        repository.save(ownerToUpdate); // em.merge()
        em.getTransaction().commit();
        em.clear();

        Owner updatedOwner = repository.findById(ownerId).get();
        assertEquals("999999", updatedOwner.getPhoneNumber(), "Phone number should be updated");
    }

    @Test
    public void deleteOwner_AttachedEntity_Success() {
        em.getTransaction().begin();
        Owner owner = new Owner.OwnerBuilder()
                .setFirstName("Andrea")
                .setLastName("Bianchi")
                .setEmail("andrea.bianchi@example.com")
                .setPhoneNumber("333111222")
                .build();
        repository.save(owner);

        repository.delete(owner); // em.remove(owner)
        em.getTransaction().commit();
        em.clear();

        Optional<Owner> deletedOwner = repository.findById(owner.getId());
        assertTrue(deletedOwner.isEmpty(), "Should be empty, the owner has been eliminated");
    }

    @Test
    public void deleteOwner_DetachedEntity_Success() {
        em.getTransaction().begin();
        Owner owner = new Owner.OwnerBuilder()
                .setFirstName("Chiara")
                .setLastName("Bianchi")
                .setEmail("chiara.bianchi@example.com")
                .setPhoneNumber("333111222")
                .build();

        repository.save(owner);
        em.getTransaction().commit();

        UUID ownerId = owner.getId();
        assertNotNull(ownerId);

        em.clear();
        em.getTransaction().begin();

        Owner ownerToDelete = repository.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("Owner not found"));

        em.clear();
        repository.delete(ownerToDelete); // em.remove(em.merge(owner))

        em.getTransaction().commit();
        em.clear();

        Optional<Owner> deletedOwner = repository.findById(ownerId);
        assertTrue(deletedOwner.isEmpty(), "Should be empty, the owner has been eliminated");
    }
}