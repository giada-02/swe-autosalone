package com.autosalone.repositories;

import com.autosalone.enums.VehicleCondition;
import com.autosalone.models.Deadline;
import com.autosalone.models.Vehicle;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DeadlineRepositoryTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private DeadlineRepository repository;

    @BeforeEach
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("autosalonePU-test");
        em = emf.createEntityManager();
        repository = new DeadlineRepository();
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
    public void saveDeadline_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle(true);
        LocalDate dueDate = LocalDate.now().plusMonths(1);
        Period recurrence = Period.ofYears(1);

        vehicle.addDeadline("Bollo Auto", dueDate, recurrence, false);
        Deadline deadline = vehicle.getDeadlines().get(0);

        repository.save(deadline);
        em.getTransaction().commit();
        assertNotNull(deadline.getId());

        em.clear();

        Optional<Deadline> found = repository.findById(deadline.getId());
        assertTrue(found.isPresent());
        assertEquals("Bollo Auto", found.get().getReason());
        assertTrue(dueDate.equals(found.get().getDueDate()));
        assertTrue(recurrence.equals(found.get().getRecurrence()));
        assertFalse(found.get().isRecalculatedFromCompletion());
    }

    @Test
    public void findById_NotFound() {
        Optional<Deadline> found = repository.findById(UUID.randomUUID());
        assertTrue(found.isEmpty(), "Should be empty, the ID does not exist");
    }

    @Test
    public void findPendingAndHistory_ByVehicleId_ReturnsCorrectLists() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle(true);

        vehicle.addDeadline("Tagliando", LocalDate.now().plusDays(10), null, false);
        vehicle.addDeadline("Cambio Gomme", LocalDate.now().minusDays(5), null, false);

        Deadline pendingDeadline = vehicle.getDeadlines().get(0);
        Deadline completedDeadline = vehicle.getDeadlines().get(1);

        repository.save(pendingDeadline);
        repository.save(completedDeadline);

        completedDeadline.complete(LocalDate.now(), "Sostituite");
        repository.save(completedDeadline);

        em.getTransaction().commit();
        em.clear();

        // findPendingByVehicleId
        List<Deadline> pendingList = repository.findPendingByVehicleId(vehicle.getId());
        assertEquals(1, pendingList.size());
        assertEquals("Tagliando", pendingList.get(0).getReason());
        assertFalse(pendingList.get(0).isCompleted());

        // findHistoryByVehicleId
        List<Deadline> historyList = repository.findHistoryByVehicleId(vehicle.getId());
        assertEquals(1, historyList.size());
        assertEquals("Cambio Gomme", historyList.get(0).getReason());
        assertTrue(historyList.get(0).isCompleted());
    }

    @Test
    public void findUrgentDeadlines_ReturnsCorrectlyFilteredAndSorted() {
        em.getTransaction().begin();
        Vehicle inShowroom = persistMockVehicle(true);
        Vehicle outOfShowroom = persistMockVehicle(false);

        // scaduta (scaduta ieri) e in salone
        inShowroom.addDeadline("Scaduta", LocalDate.now().minusDays(1), null, false);

        // imminente (scade oggi) e in salone
        inShowroom.addDeadline("Oggi", LocalDate.now(), null, false);

        // futura (scade tra 1 mese) e in salone
        inShowroom.addDeadline("Futura", LocalDate.now().plusMonths(1), null, false);

        // urgente (scaduta ieri) ma NON in salone
        outOfShowroom.addDeadline("Scaduta Fuori", LocalDate.now().minusDays(1), null, false);

        for (Deadline d : inShowroom.getDeadlines())
            repository.save(d);
        for (Deadline d : outOfShowroom.getDeadlines())
            repository.save(d);

        em.getTransaction().commit();
        em.clear();

        List<Deadline> urgent = repository.findUrgentDeadlines(LocalDate.now()); // upToDate = today

        assertEquals(2, urgent.size());
        assertEquals("Scaduta", urgent.get(0).getReason());
        assertEquals("Oggi", urgent.get(1).getReason());
    }

    @Test
    public void saveDeadline_UpdateMerge_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle(true);
        LocalDate dueDate = LocalDate.now().plusMonths(1);
        Period recurrence = Period.ofYears(1);

        vehicle.addDeadline("Bollo Auto", dueDate, recurrence, true);
        Deadline deadline = vehicle.getDeadlines().get(0);
        repository.save(deadline);
        em.getTransaction().commit();

        UUID deadlineId = deadline.getId();

        em.clear();
        em.getTransaction().begin();

        Deadline deadlineToUpdate = repository.findById(deadlineId).get();
        deadlineToUpdate.setRecalculateFromCompletion(false);

        repository.save(deadlineToUpdate); // em.merge()
        em.getTransaction().commit();
        em.clear();

        Deadline updatedDeadline = repository.findById(deadlineId).get();
        assertFalse(updatedDeadline.isRecalculatedFromCompletion(), "Recalculate from completion should be updated");
    }

    @Test
    public void deleteDeadline_AttachedEntity_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle(true);

        vehicle.addDeadline("Da Eliminare", LocalDate.now(), null, false);
        Deadline deadline = vehicle.getDeadlines().get(0);
        repository.save(deadline);

        repository.delete(deadline);
        vehicle.removeDeadline(deadline);
        em.getTransaction().commit();
        em.clear();

        Optional<Deadline> deletedDeadline = repository.findById(deadline.getId());
        assertTrue(deletedDeadline.isEmpty(), "Should be empty, the deadline has been eliminated");
    }

    @Test
    public void deleteDeadline_DetachedEntity_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = persistMockVehicle(true);

        vehicle.addDeadline("Da Eliminare", LocalDate.now(), null, false);
        Deadline deadline = vehicle.getDeadlines().get(0);
        repository.save(deadline);
        em.getTransaction().commit();

        UUID deadlineId = deadline.getId();
        assertNotNull(deadlineId);

        em.clear();
        em.getTransaction().begin();

        Deadline deadlineToDelete = repository.findById(deadlineId)
                .orElseThrow(() -> new IllegalStateException("Deadline not found"));

        em.clear();
        repository.delete(deadlineToDelete); // em.remove(em.merge(deadline))
        vehicle.removeDeadline(deadlineToDelete);

        em.getTransaction().commit();
        em.clear();

        Optional<Deadline> deletedDeadline = repository.findById(deadlineId);
        assertTrue(deletedDeadline.isEmpty(), "Should be empty, the deadline has been eliminated");
    }

    // helper method
    private Vehicle persistMockVehicle(boolean isInShowroom) {
        Vehicle v = new Vehicle.VehicleBuilder()
                .setBrand("Peugeot")
                .setModel("208")
                .setColor("Bianco")
                .setCondition(VehicleCondition.NEW)
                .setIsInShowroom(isInShowroom)
                .build();
        em.persist(v);
        return v;
    }
}