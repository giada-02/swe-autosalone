package com.autosalone.repositories;

import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Deadline;
import com.autosalone.models.Vehicle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class VehicleRepositoryTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private VehicleRepository repository;

    @BeforeEach
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("autosalonePU-test");
        em = emf.createEntityManager();
        repository = new VehicleRepository();
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
    public void saveVehicle_Success() {
        em.getTransaction().begin();

        Vehicle vehicle = new Vehicle.VehicleBuilder()
                .setBrand("Fiat")
                .setModel("Panda")
                .setColor("Rosso")
                .setCondition(VehicleCondition.SECONDHAND)
                .setIsInShowroom(true)
                .setLicensePlate("AB123CD")
                .setSellingPrice(new BigDecimal("5000.00")).build();

        repository.save(vehicle);
        em.getTransaction().commit();
        assertNotNull(vehicle.getId());

        em.clear();

        Optional<Vehicle> found = repository.findById(vehicle.getId());
        assertTrue(found.isPresent());
        assertEquals("Fiat", found.get().getBrand());
    }

    @Test
    public void findById_NotFound() {
        Optional<Vehicle> found = repository.findById(UUID.randomUUID());
        assertTrue(found.isEmpty(), "Should be empty, the ID does not exist");
    }

    @Test
    public void findVehicles_WithDynamicFilters_ReturnsCorrectResults() {
        em.getTransaction().begin();

        Vehicle v1 = new Vehicle.VehicleBuilder()
                .setBrand("BMW")
                .setModel("X5")
                .setColor("Nero")
                .setCondition(VehicleCondition.NEW)
                .setIsInShowroom(true)
                .setSellingPrice(new BigDecimal("60000.00"))
                .build();
        repository.save(v1);

        Vehicle v2 = new Vehicle.VehicleBuilder()
                .setBrand("Audi")
                .setModel("A3")
                .setColor("Bianco")
                .setCondition(VehicleCondition.SECONDHAND)
                .setIsInShowroom(false)
                .setSellingPrice(new BigDecimal("15000.00"))
                .build();
        v2.setStatus(VehicleStatus.SOLD);
        repository.save(v2);

        Vehicle v3 = new Vehicle.VehicleBuilder()
                .setBrand("BMW")
                .setModel("Serie 1")
                .setColor("Grigio")
                .setCondition(VehicleCondition.SECONDHAND)
                .setIsInShowroom(true)
                .setSellingPrice(new BigDecimal("18000.00"))
                .setLicensePlate("ZZ999XX")
                .build();
        v3.setStatus(VehicleStatus.QUOTED);
        repository.save(v3);

        em.getTransaction().commit();
        em.clear();

        List<Vehicle> byModelKeyword = repository.findVehicles("x5", null, null, null, null, null); // case-insensitive
        assertEquals(1, byModelKeyword.size(), "Should find 1 vehicle by model");
        assertEquals("X5", byModelKeyword.get(0).getModel());

        List<Vehicle> byPlateKeyword = repository.findVehicles("zz999", null, null, null, null, null);
        assertEquals(1, byPlateKeyword.size(), "Should find 1 vehicle by license plate");

        List<Vehicle> byBrand = repository.findVehicles(null, "BMW", null, null, null, null);
        assertEquals(2, byBrand.size(), "Should find 2 vehicles with 'BMW' as brand");

        List<Vehicle> byCondition = repository.findVehicles(null, null, VehicleCondition.NEW, null, null, null);
        assertEquals(1, byCondition.size(), "Should find 1 vehicle with 'NEW' as condition");

        List<Vehicle> maxPrice = repository.findVehicles(null, null, null, new BigDecimal("20000.00"), null,
                null);
        assertEquals(2, maxPrice.size(), "Should find 2 vehicles with selling price under 20000");

        List<Vehicle> inShowroom = repository.findVehicles(null, null, null, null, true, null);
        assertEquals(2, inShowroom.size(), "Should find 2 vehicles in showroom");

        List<VehicleStatus> statuses = Arrays.asList(VehicleStatus.QUOTED, VehicleStatus.SOLD);

        List<Vehicle> combined = repository.findVehicles(null, "BMW", VehicleCondition.SECONDHAND,
                new BigDecimal("20000.00"), true, statuses);
        assertEquals(1, combined.size(), "Should find exactly 1 vehicle matching all combined criteria");
        assertEquals("Serie 1", combined.get(0).getModel());
    }

    @Test
    public void findAvailableForSale_ReturnsOnlyAvailableAndQuoted() {
        em.getTransaction().begin();

        Vehicle v1 = new Vehicle.VehicleBuilder().setBrand("V1").setModel("M1").setColor("C1")
                .setCondition(VehicleCondition.NEW).setIsInShowroom(true).build();
        repository.save(v1); // AVAILABLE

        Vehicle v2 = new Vehicle.VehicleBuilder().setBrand("V2").setModel("M2").setColor("C2")
                .setCondition(VehicleCondition.NEW).setIsInShowroom(true).build();
        v2.setStatus(VehicleStatus.QUOTED);
        repository.save(v2);

        Vehicle v3 = new Vehicle.VehicleBuilder().setBrand("V3").setModel("M3").setColor("C3")
                .setCondition(VehicleCondition.NEW).setIsInShowroom(true).build();
        v3.setStatus(VehicleStatus.RESERVED);
        repository.save(v3);

        Vehicle v4 = new Vehicle.VehicleBuilder().setBrand("V4").setModel("M4").setColor("C4")
                .setCondition(VehicleCondition.NEW).setIsInShowroom(true).build();
        v4.setStatus(VehicleStatus.SOLD);
        repository.save(v4);

        em.getTransaction().commit();
        em.clear();

        List<Vehicle> availableCars = repository.findAvailableForSale();
        assertEquals(2, availableCars.size(), "Should only return AVAILABLE and QUOTED vehicles");

        boolean hasReservedOrSold = availableCars.stream()
                .anyMatch(v -> v.getStatus() == VehicleStatus.RESERVED
                        || v.getStatus() == VehicleStatus.SOLD);
        assertFalse(hasReservedOrSold, "Should not contain RESERVED or SOLD vehicles");
    }

    @Test
    public void findAllBrands_ReturnsDistinctAndSortedBrands() {
        em.getTransaction().begin();

        repository.save(new Vehicle.VehicleBuilder().setBrand("Toyota").setModel("Yaris").setColor("Bianco")
                .setCondition(VehicleCondition.NEW).setIsInShowroom(true).build());
        repository.save(new Vehicle.VehicleBuilder().setBrand("Alfa Romeo").setModel("Giulia").setColor("Rosso")
                .setCondition(VehicleCondition.NEW).setIsInShowroom(true).build());
        repository.save(new Vehicle.VehicleBuilder().setBrand("Toyota").setModel("Aygo").setColor("Nero")
                .setCondition(VehicleCondition.NEW).setIsInShowroom(true).build());
        repository.save(new Vehicle.VehicleBuilder().setBrand("Honda").setModel("Civic").setColor("Grigio")
                .setCondition(VehicleCondition.NEW).setIsInShowroom(true).build());

        em.getTransaction().commit();
        em.clear();

        List<String> brands = repository.findAllBrands();

        assertEquals(3, brands.size(), "Should return exactly 3 distinct brands");
        assertEquals("Alfa Romeo", brands.get(0), "Should be sorted alphabetically");
        assertEquals("Honda", brands.get(1));
        assertEquals("Toyota", brands.get(2));
    }

    @Test
    public void saveVehicle_UpdateMerge_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = new Vehicle.VehicleBuilder()
                .setBrand("Ford")
                .setModel("Fiesta")
                .setColor("Blu")
                .setCondition(VehicleCondition.SECONDHAND)
                .setIsInShowroom(true)
                .setSellingPrice(new BigDecimal("8000.00"))
                .build();
        repository.save(vehicle);
        em.getTransaction().commit();

        UUID vehicleId = vehicle.getId();

        em.clear();
        em.getTransaction().begin();

        Vehicle vehicleToUpdate = repository.findById(vehicleId).get();
        vehicleToUpdate.setColor("Nero");

        repository.save(vehicleToUpdate); // em.merge()
        em.getTransaction().commit();
        em.clear();

        Vehicle updatedVehicle = repository.findById(vehicleId).get();
        assertEquals("Nero", updatedVehicle.getColor(), "Color should be updated");
    }

    @Test
    public void deleteVehicle_AttachedEntity_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = new Vehicle.VehicleBuilder()
                .setBrand("Renault")
                .setModel("Clio")
                .setColor("Bianco")
                .setCondition(VehicleCondition.NEW)
                .setIsInShowroom(true)
                .build();
        repository.save(vehicle);

        repository.delete(vehicle); // em.remove(vehicle)
        em.getTransaction().commit();
        em.clear();

        Optional<Vehicle> deletedVehicle = repository.findById(vehicle.getId());
        assertTrue(deletedVehicle.isEmpty(), "Should be empty, the vehicle has been eliminated");
    }

    @Test
    public void deleteVehicle_DetachedEntity_Success() {
        em.getTransaction().begin();
        Vehicle vehicle = new Vehicle.VehicleBuilder()
                .setBrand("Peugeot")
                .setModel("208")
                .setColor("Grigio")
                .setCondition(VehicleCondition.NEW)
                .setIsInShowroom(true)
                .build();

        repository.save(vehicle);
        em.getTransaction().commit();

        UUID vehicleId = vehicle.getId();
        assertNotNull(vehicleId);

        em.clear();
        em.getTransaction().begin();

        Vehicle vehicleToDelete = repository.findById(vehicleId)
                .orElseThrow(() -> new IllegalStateException("Vehicle not found"));

        em.clear();
        repository.delete(vehicleToDelete); // em.remove(em.merge(vehicle))

        em.getTransaction().commit();
        em.clear();

        Optional<Vehicle> deletedVehicle = repository.findById(vehicleId);
        assertTrue(deletedVehicle.isEmpty(), "Should be empty, the vehicle has been eliminated");
    }

    @Test
    public void deleteVehicle_CascadesToDeadlines() {
        em.getTransaction().begin();
        Vehicle vehicle = new Vehicle.VehicleBuilder()
                .setBrand("Ford")
                .setModel("Focus")
                .setColor("Grigio")
                .setCondition(VehicleCondition.NEW)
                .setIsInShowroom(true)
                .build();

        vehicle.addDeadline("Assicurazione", LocalDate.now().plusMonths(6), null, false);

        repository.save(vehicle);
        em.getTransaction().commit();

        UUID vehicleId = vehicle.getId();
        UUID deadlineId = vehicle.getDeadlines().get(0).getId();

        em.clear();
        em.getTransaction().begin();

        Vehicle vehicleToDelete = repository.findById(vehicleId).get();
        repository.delete(vehicleToDelete);

        em.getTransaction().commit();
        em.clear();

        Optional<Vehicle> deletedVehicle = repository.findById(vehicleId);
        assertTrue(deletedVehicle.isEmpty(), "The vehicle should be deleted");

        Deadline deletedDeadline = em.find(Deadline.class, deadlineId);
        assertNull(deletedDeadline, "The associated deadline should be deleted via cascade");
    }

}