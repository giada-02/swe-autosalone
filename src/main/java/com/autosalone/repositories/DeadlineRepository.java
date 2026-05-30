package com.autosalone.repositories;

import com.autosalone.models.Deadline;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DeadlineRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Deadline> findById(UUID id) {
        Deadline deadline = em.find(Deadline.class, id);
        return Optional.ofNullable(deadline);
    }

    // trova le scadenze non ancora completate per un veicolo
    public List<Deadline> findPendingByVehicleId(UUID vehicleId) {
        return em.createQuery(
                "SELECT d FROM Deadline d WHERE d.vehicle.id = :vehicleId AND d.isCompleted = false ORDER BY d.dueDate ASC",
                Deadline.class)
                .setParameter("vehicleId", vehicleId)
                .getResultList();
    }

    // trova lo storico delle manutenzioni completate del veicolo
    public List<Deadline> findHistoryByVehicleId(UUID vehicleId) {
        return em.createQuery(
                "SELECT d FROM Deadline d WHERE d.vehicle.id = :vehicleId AND d.isCompleted = true ORDER BY d.completionDate DESC",
                Deadline.class)
                .setParameter("vehicleId", vehicleId)
                .getResultList();
    }

    // trova tutte le scadenze imminenti o già passate delle auto in salone
    public List<Deadline> findUrgentDeadlines(LocalDate upToDate) {
        return em.createQuery(
                "SELECT d FROM Deadline d WHERE d.isCompleted = false AND d.dueDate <= :upToDate AND d.vehicle.isInShowroom = true ORDER BY d.dueDate ASC",
                Deadline.class)
                .setParameter("upToDate", upToDate)
                .getResultList();
    }

    public Deadline save(Deadline deadline) {
        if (deadline.getId() == null) {
            em.persist(deadline);
            return deadline;
        } else {
            return em.merge(deadline);
        }
    }

    public void delete(Deadline deadline) {
        if (em.contains(deadline)) {
            em.remove(deadline);
        } else {
            em.remove(em.merge(deadline));
        }
    }
}