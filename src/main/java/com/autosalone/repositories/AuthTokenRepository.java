package com.autosalone.repositories;

import java.util.Optional;

import com.autosalone.models.AuthToken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class AuthTokenRepository {

    @Inject
    protected EntityManager em;

    public Optional<AuthToken> findByToken(String token) {
        return em.createQuery("SELECT t FROM AuthToken t WHERE t.token = :token", AuthToken.class)
                .setParameter("token", token)
                .getResultStream()
                .findFirst();
    }

    public void save(AuthToken token) {
        if (token.getId() == null) {
            em.persist(token);
        } else {
            em.merge(token);
        }
    }

    public void delete(AuthToken token) {
        em.remove(em.contains(token) ? token : em.merge(token));
    }
}