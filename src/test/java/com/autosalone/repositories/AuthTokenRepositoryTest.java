package com.autosalone.repositories;

import com.autosalone.enums.TokenType;
import com.autosalone.models.AuthToken;
import com.autosalone.models.Customer;
import com.autosalone.models.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuthTokenRepositoryTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private AuthTokenRepository repository;

    @BeforeEach
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("autosalonePU-test");
        em = emf.createEntityManager();
        repository = new AuthTokenRepository();
        repository.em = this.em;
    }

    @AfterEach
    public void tearDown() {
        if (em != null)
            em.close();
        if (emf != null)
            emf.close();
    }

    @Test
    public void saveAuthToken_Success() {
        em.getTransaction().begin();
        User user = persistMockUser();

        AuthToken token = new AuthToken("secure_token_123", user, TokenType.REGISTRATION,
                Instant.now().plus(48, ChronoUnit.HOURS));

        repository.save(token);
        em.getTransaction().commit();

        assertNotNull(token.getId());

        em.clear();

        Optional<AuthToken> found = repository.findByToken("secure_token_123");
        assertTrue(found.isPresent());
        assertEquals("secure_token_123", found.get().getToken());
        assertEquals(TokenType.REGISTRATION, found.get().getType());
        assertEquals(user.getId(), found.get().getUser().getId());
    }

    @Test
    public void deleteAuthToken_AttachedEntity_Success() {
        em.getTransaction().begin();
        User user = persistMockUser();
        AuthToken token = new AuthToken("token_to_delete", user, TokenType.PASSWORD_RESET,
                Instant.now().plus(2, ChronoUnit.HOURS));

        repository.save(token);

        repository.delete(token);

        em.getTransaction().commit();
        em.clear();

        Optional<AuthToken> deletedToken = repository.findByToken("token_to_delete");
        assertTrue(deletedToken.isEmpty(), "Should be empty, the token has been eliminated");
    }

    @Test
    public void deleteAuthToken_DetachedEntity_Success() {
        em.getTransaction().begin();
        User user = persistMockUser();
        AuthToken token = new AuthToken("detached_token", user, TokenType.PASSWORD_RESET,
                Instant.now().plus(2, ChronoUnit.HOURS));
        repository.save(token);
        em.getTransaction().commit();

        em.clear();

        em.getTransaction().begin();

        AuthToken tokenToDelete = repository.findByToken("detached_token")
                .orElseThrow(() -> new IllegalStateException("Token not found"));
        em.clear();

        repository.delete(tokenToDelete);
        em.getTransaction().commit();
        em.clear();

        Optional<AuthToken> deletedToken = repository.findByToken("detached_token");
        assertTrue(deletedToken.isEmpty(), "Should be empty, the token has been eliminated");
    }

    // helper method
    private User persistMockUser() {
        Customer customer = new Customer.CustomerBuilder()
                .setFirstName("Mario")
                .setLastName("Rossi")
                .setPhoneNumber("3331234567")
                .build();
        em.persist(customer);
        return customer;
    }
}