package com.autosalone.repositories;

import com.autosalone.models.catalog.PurchasableItem;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CatalogRepository {

    @PersistenceContext
    protected EntityManager em;

    public Optional<PurchasableItem> findById(UUID id) {
        PurchasableItem purchasableItem = em.find(PurchasableItem.class, id);
        return Optional.ofNullable(purchasableItem);
    }

    public List<PurchasableItem> findPurchasableItems(String keyword, Boolean isArchived,
            Class<? extends PurchasableItem> itemType) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM PurchasableItem p WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();

        if (keyword != null && !keyword.isEmpty()) {
            jpql.append(" AND LOWER(p.name) LIKE LOWER(:keyword)");
            parameters.put("keyword", "%" + keyword + "%");
        }

        if (isArchived != null) {
            jpql.append(" AND p.isArchived = :isArchived");
            parameters.put("isArchived", isArchived);
        }

        if (itemType != null) {
            jpql.append(" AND TYPE(p) = :itemType");
            parameters.put("itemType", itemType);
        }

        jpql.append(" ORDER BY p.name ASC");

        TypedQuery<PurchasableItem> query = em.createQuery(jpql.toString(), PurchasableItem.class);
        parameters.forEach(query::setParameter);

        return query.getResultList();
    }

    public boolean isItemInUse(UUID itemId) {
        Long documentCount = em.createQuery(
                "SELECT COUNT(d) FROM SalesDocument d JOIN d.items i WHERE i.item.id = :itemId", Long.class)
                .setParameter("itemId", itemId)
                .getSingleResult();

        Long packageCount = em.createQuery(
                "SELECT COUNT(p) FROM AccessoryPackage p JOIN p.items i WHERE i.id = :itemId", Long.class)
                .setParameter("itemId", itemId)
                .getSingleResult();

        return documentCount > 0 || packageCount > 0;
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