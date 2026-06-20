package com.autosalone.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.autosalone.models.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class UserRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<User> findById(UUID id) {
        User user = em.find(User.class, id);
        return Optional.ofNullable(user);
    }

    public Optional<User> findByEmail(String email) {
        return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    public List<User> findUsers() {
        return em.createQuery("SELECT u FROM User u ORDER BY u.lastName ASC, u.firstName ASC", User.class)
                .getResultList();
    }
}
