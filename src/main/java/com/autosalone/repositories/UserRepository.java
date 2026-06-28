package com.autosalone.repositories;

import java.util.Optional;
import java.util.UUID;

import com.autosalone.models.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class UserRepository {

    @PersistenceContext
    protected EntityManager em;

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

    public User save(User user) {
        if (user.getId() == null) {
            em.persist(user);
            return user;
        } else {
            return em.merge(user);
        }
    }

    public void delete(User user) {
        if (em.contains(user)) {
            em.remove(user);
        } else {
            em.remove(em.merge(user));
        }
    }
}
