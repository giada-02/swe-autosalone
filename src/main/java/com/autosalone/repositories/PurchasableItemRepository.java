package com.autosalone.repositories;

import com.autosalone.models.catalog.PurchasableItem;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PurchasableItemRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<PurchasableItem> findById(UUID id) {
        PurchasableItem purchasableItem = em.find(PurchasableItem.class, id);
        return Optional.ofNullable(purchasableItem);
    }

    public List<PurchasableItem> findPurchasableItems(String keyword) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM PurchasableItem p WHERE 1=1");

        if (keyword != null && !keyword.isEmpty())
            jpql.append(" AND LOWER(p.name) LIKE LOWER(:keyword)");

        jpql.append(" ORDER BY p.name ASC");

        TypedQuery<PurchasableItem> query = em.createQuery(jpql.toString(), PurchasableItem.class);

        if (keyword != null && !keyword.isEmpty())
            query.setParameter("keyword", "%" + keyword + "%");

        return query.getResultList();
    }

    public PurchasableItem save(PurchasableItem item) {
        if (item.getId() == null) {
            em.persist(item);
            return item;
        } else {
            return em.merge(item);
        }
    }

    public void delete(PurchasableItem item) {
        if (em.contains(item)) {
            em.remove(item);
        } else {
            em.remove(em.merge(item));
        }
    }
}