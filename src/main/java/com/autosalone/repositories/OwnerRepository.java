package com.autosalone.repositories;

import com.autosalone.models.Owner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OwnerRepository {

    @PersistenceContext
    protected EntityManager em;

    public Optional<Owner> findById(UUID id) {
        Owner owner = em.find(Owner.class, id);
        return Optional.ofNullable(owner);
    }

    public Optional<Owner> findByEmail(String email) {
        return em.createQuery("SELECT o FROM Owner o WHERE o.email = :email", Owner.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    public List<Owner> findOwners() {
        return em.createQuery("SELECT o FROM Owner o ORDER BY o.lastName ASC, o.firstName ASC",
                Owner.class).getResultList();
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