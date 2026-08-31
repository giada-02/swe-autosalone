package com.autosalone.repositories;

import com.autosalone.models.Owner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class OwnerRepository {

    @PersistenceContext
    protected EntityManager em;

    public Optional<Owner> findById(UUID id) {
        Owner owner = em.find(Owner.class, id);
        return Optional.ofNullable(owner);
    }

    public List<Owner> findOwners(Boolean isActive) {
        StringBuilder jpql = new StringBuilder("SELECT o FROM Owner o WHERE 1=1");

        if (isActive != null)
            jpql.append(" AND o.isActive = :isActive");

        jpql.append(" ORDER BY o.lastName ASC, o.firstName ASC");
        TypedQuery<Owner> query = em.createQuery(jpql.toString(), Owner.class);

        if (isActive != null)
            query.setParameter("isActive", isActive);

        return query.getResultList();
    }

    public Set<UUID> filterOwnerIds(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty())
            return Set.of();

        List<UUID> ownerIds = em.createQuery("SELECT o.id FROM Owner o WHERE o.id IN :ids", UUID.class)
                .setParameter("ids", userIds)
                .getResultList();

        return new HashSet<>(ownerIds);
    }

    public Owner save(Owner owner) {
        if (owner.getId() == null) {
            em.persist(owner);
            return owner;
        } else {
            return em.merge(owner);
        }
    }

    public void delete(Owner owner) {
        if (em.contains(owner)) {
            em.remove(owner);
        } else {
            em.remove(em.merge(owner));
        }
    }
}