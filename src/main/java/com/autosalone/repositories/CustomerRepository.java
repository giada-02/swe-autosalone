package com.autosalone.repositories;

import com.autosalone.models.Customer;

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
public class CustomerRepository {

    @PersistenceContext
    protected EntityManager em;

    public Optional<Customer> findById(UUID id) {
        Customer customer = em.find(Customer.class, id);
        return Optional.ofNullable(customer);
    }

    public List<Customer> findCustomers(
            String keyword, Boolean isActive) {

        StringBuilder jpql = new StringBuilder("SELECT c FROM Customer c WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();

        if (keyword != null) {
            jpql.append(
                    " AND (LOWER(c.firstName) LIKE LOWER(:keyword) OR LOWER(c.lastName) LIKE LOWER(:keyword) OR LOWER(c.phoneNumber) LIKE LOWER(:keyword) OR LOWER(c.email) LIKE LOWER(:keyword))");
            parameters.put("keyword", "%" + keyword + "%");
        }

        if (isActive != null) {
            jpql.append(" AND c.isActive = :isActive");
            parameters.put("isActive", isActive);
        }

        jpql.append(" ORDER BY c.lastName ASC, c.firstName ASC");

        TypedQuery<Customer> query = em.createQuery(jpql.toString(), Customer.class);
        parameters.forEach(query::setParameter);

        return query.getResultList();
    }

    public Customer save(Customer customer) {
        if (customer.getId() == null) {
            em.persist(customer);
            return customer;
        } else {
            return em.merge(customer);
        }
    }

    public void delete(Customer customer) {
        if (em.contains(customer)) {
            em.remove(customer);
        } else {
            em.remove(em.merge(customer));
        }
    }
}