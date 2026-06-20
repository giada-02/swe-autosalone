package com.autosalone.repositories;

import com.autosalone.enums.SortOrder;
import com.autosalone.enums.TransactionType;
import com.autosalone.models.Transaction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TransactionRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Transaction> findById(UUID id) {
        Transaction transaction = em.find(Transaction.class, id);
        return Optional.ofNullable(transaction);
    }

    public List<Transaction> findTransactions(
            LocalDate dateFrom,
            LocalDate dateTo,
            TransactionType type,
            SortOrder sortOrder) {

        StringBuilder jpql = new StringBuilder("SELECT t FROM Transaction t WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();

        if (dateFrom != null) {
            jpql.append(" AND t.date >= :from");
            parameters.put("from", dateFrom);
        }
        if (dateTo != null) {
            jpql.append(" AND t.date <= :to");
            parameters.put("to", dateTo);
        }
        if (type != null) {
            jpql.append(" AND t.type = :type");
            parameters.put("type", type);
        }

        String direction = (sortOrder == SortOrder.ASC) ? "ASC" : "DESC";
        jpql.append(" ORDER BY t.date ").append(direction);

        TypedQuery<Transaction> query = em.createQuery(jpql.toString(), Transaction.class);
        parameters.forEach(query::setParameter);

        return query.getResultList();
    }

    public List<Transaction> findAllExpenses(UUID vehicleId) {
        return em.createQuery("SELECT t FROM Transaction t WHERE t.vehicle.id = :vehicleId ORDER BY t.date DESC",
                Transaction.class)
                .setParameter("vehicleId", vehicleId)
                .getResultList();
    }

    public List<Transaction> findAllPayments(UUID contractId) {
        return em.createQuery("SELECT t FROM Transaction t WHERE t.contract.id = :contractId ORDER BY t.date DESC",
                Transaction.class)
                .setParameter("contractId", contractId)
                .getResultList();
    }

    public BigDecimal sumInTransactions(LocalDate dateFrom, LocalDate dateTo) {
        return calculateSum(dateFrom, dateTo, TransactionType.IN);
    }

    public BigDecimal sumOutTransactions(LocalDate dateFrom, LocalDate dateTo) {
        return calculateSum(dateFrom, dateTo, TransactionType.OUT);
    }

    private BigDecimal calculateSum(LocalDate dateFrom, LocalDate dateTo, TransactionType type) {
        StringBuilder jpql = new StringBuilder("SELECT SUM(t.amount) FROM Transaction t WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();

        if (type != null) {
            jpql.append(" AND t.type = :type");
            parameters.put("type", type);
        }

        if (dateFrom != null) {
            jpql.append(" AND t.date >= :from");
            parameters.put("from", dateFrom);
        }

        if (dateTo != null) {
            jpql.append(" AND t.date <= :to");
            parameters.put("to", dateTo);
        }

        TypedQuery<BigDecimal> query = em.createQuery(jpql.toString(), BigDecimal.class);
        parameters.forEach(query::setParameter);

        BigDecimal result = query.getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    public Transaction save(Transaction transaction) {
        if (transaction.getId() == null) {
            em.persist(transaction);
            return transaction;
        } else {
            return em.merge(transaction);
        }
    }

}
