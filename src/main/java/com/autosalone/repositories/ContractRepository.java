package com.autosalone.repositories;

import com.autosalone.enums.ContractStatus;
import com.autosalone.models.Contract;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ContractRepository {

    @PersistenceContext
    protected EntityManager em;

    public Optional<Contract> findById(UUID id) {
        Contract contract = em.find(Contract.class, id);
        return Optional.ofNullable(contract);
    }

    public List<Contract> findContracts(LocalDate dateFrom, LocalDate dateTo, Boolean IsArchived, UUID vehicleId,
            UUID customerId, List<ContractStatus> statusList) {

        StringBuilder jpql = new StringBuilder("SELECT c FROM Contract c WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();

        if (dateFrom != null) {
            jpql.append(" AND c.date >= :from");
            parameters.put("from", dateFrom);
        }
        if (dateTo != null) {
            jpql.append(" AND c.date <= :to");
            parameters.put("to", dateTo);
        }
        if (IsArchived != null) {
            jpql.append(" AND c.isArchived = :IsArchived");
            parameters.put("IsArchived", IsArchived);
        }
        if (vehicleId != null) {
            jpql.append(" AND c.vehicle.id = :vehicleId");
            parameters.put("vehicleId", vehicleId);
        }
        if (customerId != null) {
            jpql.append(" AND c.customer.id = :customerId");
            parameters.put("customerId", customerId);
        }
        if (statusList != null && !statusList.isEmpty()) {
            jpql.append(" AND c.status IN :statusList");
            parameters.put("statusList", statusList);
        }

        jpql.append(" ORDER BY c.date DESC, c.createdAt DESC");

        TypedQuery<Contract> query = em.createQuery(jpql.toString(), Contract.class);
        parameters.forEach(query::setParameter);

        return query.getResultList();
    }

    public List<Contract> findVisibleContractsByCustomerId(UUID customerId) {
        return em.createQuery(
                "SELECT c FROM Contract c WHERE c.customer.id = :customerId AND c.status != :draftStatus ORDER BY c.date DESC, c.createdAt DESC",
                Contract.class)
                .setParameter("customerId", customerId)
                .setParameter("draftStatus", ContractStatus.DRAFT)
                .getResultList();
    }

    public List<Contract> findConflictingContractsForVehicle(UUID vehicleId, UUID excludeContractId) {
        StringBuilder jpql = new StringBuilder(
                "SELECT c FROM Contract c WHERE c.vehicle.id = :vehicleId AND c.status = :draft");

        if (excludeContractId != null) {
            jpql.append(" AND c.id != :excludeId");
        }

        TypedQuery<Contract> query = em.createQuery(jpql.toString(), Contract.class)
                .setParameter("vehicleId", vehicleId)
                .setParameter("draft", ContractStatus.DRAFT);

        if (excludeContractId != null) {
            query.setParameter("excludeId", excludeContractId);
        }

        return query.getResultList();
    }

    public Contract save(Contract contract) {
        if (contract.getId() == null) {
            em.persist(contract);
            return contract;
        } else {
            return em.merge(contract);
        }
    }

    public void delete(Contract contract) {
        if (em.contains(contract)) {
            em.remove(contract);
        } else {
            em.remove(em.merge(contract));
        }
    }
}