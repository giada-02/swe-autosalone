package com.autosalone.repositories;

import java.time.LocalDateTime;
import java.util.Optional;

import com.autosalone.enums.TokenType;
import com.autosalone.models.AuthToken;
import com.autosalone.models.User;

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

    public Optional<AuthToken> findByUserAndType(User user, TokenType type) {
        return em.createQuery("SELECT t FROM AuthToken t WHERE t.user = :user AND t.type = :type", AuthToken.class)
                .setParameter("user", user)
                .setParameter("type", type)
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

    public int deleteAllExpiredTokens(LocalDateTime now) {
        return em.createQuery("DELETE FROM AuthToken t WHERE t.expiryDate < :now")
                .setParameter("now", now)
                .executeUpdate();
    }
}