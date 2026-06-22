package com.autosalone.repositories;

import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Vehicle;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class VehicleRepository {

    @PersistenceContext
    protected EntityManager em;

    public Optional<Vehicle> findById(UUID id) {
        Vehicle vehicle = em.find(Vehicle.class, id);
        return Optional.ofNullable(vehicle);
    }

    public List<Vehicle> findVehicles(
            String keyword,
            String brand,
            VehicleCondition condition,
            BigDecimal maxPrice,
            Boolean isInShowroom,
            List<VehicleStatus> statusList) {

        StringBuilder jpql = new StringBuilder("SELECT v FROM Vehicle v WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();

        if (keyword != null && !keyword.isEmpty()) {
            jpql.append(
                    " AND (LOWER(v.brand) LIKE LOWER(:keyword) OR LOWER(v.model) LIKE LOWER(:keyword) OR LOWER(v.licensePlate) LIKE LOWER(:keyword))");
            parameters.put("keyword", "%" + keyword + "%");
        }

        if (brand != null && !brand.trim().isEmpty()) {
            jpql.append(" AND v.brand = :brand");
            parameters.put("brand", brand);
        }
        if (condition != null) {
            jpql.append(" AND v.condition = :condition");
            parameters.put("condition", condition);
        }
        if (maxPrice != null) {
            jpql.append(" AND v.sellingPrice <= :maxPrice");
            parameters.put("maxPrice", maxPrice);
        }
        if (isInShowroom != null) {
            jpql.append(" AND v.isInShowroom = :isInShowroom");
            parameters.put("isInShowroom", isInShowroom);
        }
        if (statusList != null && !statusList.isEmpty()) {
            jpql.append(" AND v.status IN :statusList");
            parameters.put("statusList", statusList);
        }

        jpql.append(" ORDER BY v.createdAt DESC");

        TypedQuery<Vehicle> query = em.createQuery(jpql.toString(), Vehicle.class);
        parameters.forEach(query::setParameter);

        return query.getResultList();
    }

    public List<Vehicle> findAvailableForSale() {
        return em.createQuery(
                "SELECT v FROM Vehicle v WHERE v.status IN (:available, :quoted)",
                Vehicle.class)
                .setParameter("available", VehicleStatus.AVAILABLE)
                .setParameter("quoted", VehicleStatus.QUOTED)
                .getResultList();
    }

    public List<String> findAllBrands() {
        return em.createQuery("SELECT DISTINCT v.brand FROM Vehicle v ORDER BY v.brand ASC", String.class)
                .getResultList();
    }

    public Vehicle save(Vehicle vehicle) {
        if (vehicle.getId() == null) {
            em.persist(vehicle);
            return vehicle;
        } else {
            return em.merge(vehicle);
        }
    }

    public void delete(Vehicle vehicle) {
        if (em.contains(vehicle)) {
            em.remove(vehicle);
        } else {
            em.remove(em.merge(vehicle));
        }
    }
}