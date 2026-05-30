package com.autosalone.repositories;

import com.autosalone.enums.ContractStatus;
import com.autosalone.models.Contract;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ContractRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Contract> findById(UUID id) {
        Contract contract = em.find(Contract.class, id);
        return Optional.ofNullable(contract);
    }

    public List<Contract> findContracts(List<ContractStatus> statusList) {

        StringBuilder jpql = new StringBuilder("SELECT c FROM Contract c WHERE 1=1");

        if (statusList != null && !statusList.isEmpty()) {
            jpql.append(" AND c.status IN :statusList");
        }

        jpql.append(" ORDER BY c.date DESC");

        TypedQuery<Contract> query = em.createQuery(jpql.toString(), Contract.class);

        if (statusList != null && !statusList.isEmpty()) {
            query.setParameter("statusList", statusList);
        }

        return query.getResultList();
    }

    public List<Contract> findByVehicleId(UUID vehicleId) {
        return em.createQuery("SELECT c FROM Contract c WHERE c.vehicle.id = :vehicleId ORDER BY c.date DESC",
                Contract.class)
                .setParameter("vehicleId", vehicleId)
                .getResultList();
    }

    public List<Contract> findByCustomerId(UUID customerId) {
        return em.createQuery("SELECT c FROM Contract c WHERE c.customer.id = :customerId ORDER BY c.date DESC",
                Contract.class)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    public List<Contract> findVisibleContractsByCustomerId(UUID customerId) {
        return em.createQuery(
                "SELECT c FROM Contract c WHERE c.customer.id = :customerId AND c.status != :draftStatus ORDER BY c.date DESC",
                Contract.class)
                .setParameter("customerId", customerId)
                .setParameter("draftStatus", ContractStatus.DRAFT)
                .getResultList();
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