package com.autosalone.repositories;

import com.autosalone.enums.QuotationStatus;
import com.autosalone.models.Quotation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class QuotationRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Quotation> findById(UUID id) {
        Quotation quotation = em.find(Quotation.class, id);
        return Optional.ofNullable(quotation);
    }

    public List<Quotation> findQuotations(
            List<QuotationStatus> statusList) {

        StringBuilder jpql = new StringBuilder("SELECT q FROM Quotation q WHERE 1=1");

        if (statusList != null && !statusList.isEmpty())
            jpql.append(" AND q.status IN :statusList");

        jpql.append(" ORDER BY q.date DESC");

        TypedQuery<Quotation> query = em.createQuery(jpql.toString(), Quotation.class);

        if (statusList != null && !statusList.isEmpty())
            query.setParameter("statusList", statusList);

        return query.getResultList();
    }

    public List<Quotation> findByVehicleId(UUID vehicleId) {
        return em.createQuery("SELECT q FROM Quotation q WHERE q.vehicle.id = :vehicleId ORDER BY q.date DESC",
                Quotation.class)
                .setParameter("vehicleId", vehicleId)
                .getResultList();
    }

    public List<Quotation> findByCustomerId(UUID customerId) {
        return em.createQuery("SELECT q FROM Quotation q WHERE q.customer.id = :customerId ORDER BY q.date DESC",
                Quotation.class)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    public List<Quotation> findVisibleQuotationsByCustomerId(UUID customerId) {
        return em.createQuery(
                "SELECT q FROM Quotation q WHERE q.customer.id = :customerId AND q.status != :draftStatus ORDER BY q.date DESC",
                Quotation.class)
                .setParameter("customerId", customerId)
                .setParameter("draftStatus", QuotationStatus.DRAFT)
                .getResultList();
    }

    public List<Quotation> findExpiredQuotations(LocalDate currentDate) {
        return em.createQuery(
                "SELECT q FROM Quotation q WHERE q.status = :status AND q.expirationDate < :date",
                Quotation.class)
                .setParameter("status", QuotationStatus.ISSUED)
                .setParameter("date", currentDate)
                .getResultList();
    }

    public List<Quotation> findConflictingQuotationsForVehicle(UUID vehicleId,
            UUID excludeQuotationId) {

        StringBuilder jpql = new StringBuilder(
                "SELECT q FROM Quotation q WHERE q.vehicle.id = :vehicleId AND q.status IN (:draft, :issued)");

        if (excludeQuotationId != null) {
            jpql.append(" AND q.id != :excludeId");
        }

        TypedQuery<Quotation> query = em.createQuery(jpql.toString(), Quotation.class)
                .setParameter("vehicleId", vehicleId)
                .setParameter("draft", QuotationStatus.DRAFT)
                .setParameter("issued", QuotationStatus.ISSUED);

        if (excludeQuotationId != null) {
            query.setParameter("excludeId", excludeQuotationId);
        }

        return query.getResultList();
    }

    public Quotation save(Quotation quotation) {
        if (quotation.getId() == null) {
            em.persist(quotation);
            return quotation;
        } else {
            return em.merge(quotation);
        }
    }

    public void delete(Quotation quotation) {
        if (em.contains(quotation)) {
            em.remove(quotation);
        } else {
            em.remove(em.merge(quotation));
        }
    }
}