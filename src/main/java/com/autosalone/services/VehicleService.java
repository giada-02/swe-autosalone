package com.autosalone.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.DeadlineCreateRequest;
import com.autosalone.dtos.VehicleCreateRequest;
import com.autosalone.dtos.VehicleUpdateRequest;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Deadline;
import com.autosalone.models.Transaction;
import com.autosalone.models.TransactionFactory;
import com.autosalone.models.Vehicle;
import com.autosalone.repositories.DeadlineRepository;
import com.autosalone.repositories.VehicleRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class VehicleService {

    @Inject
    private VehicleRepository vehicleRepository;

    @Inject
    private DeadlineRepository deadlineRepository;

    // read

    public Vehicle getVehicleById(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found of id: " + id));
    }

    public List<Vehicle> getVehicles(String keyword, String brand, VehicleCondition condition,
            BigDecimal maxPrice, Boolean isInShowroom, List<VehicleStatus> statusList) {
        return vehicleRepository.findVehicles(keyword, brand, condition, maxPrice, isInShowroom, statusList);
    }

    public List<String> getAllBrands() {
        return vehicleRepository.findAllBrands();
    }

    // write

    @Transactional
    public UUID addVehicle(VehicleCreateRequest request) {

        Vehicle newVehicle = new Vehicle.VehicleBuilder()
                .setBrand(request.brand())
                .setModel(request.model())
                .setColor(request.color())
                .setCondition(request.condition())
                .setSellingPrice(request.sellingPrice())
                .setHandoverDate(request.handoverDate())
                .setLicensePlate(request.licensePlate())
                .setRegistrationDate(request.registrationDate())
                .setKilometers(request.kilometers())
                .setIsInShowroom(request.inShowroom())
                .build();

        if (request.purchaseTransactionAmount() != null && request.purchaseTransactionDate() != null) {
            Transaction purchase = TransactionFactory.createVehiclePurchase(newVehicle,
                    request.purchaseTransactionAmount(), request.purchaseTransactionDate());
            newVehicle.setPurchaseTransaction(purchase);
        }

        vehicleRepository.save(newVehicle);
        return newVehicle.getId();
    }

    @Transactional
    public void withdrawVehicle(UUID vehicleId, String reason) {
        Vehicle vehicle = getVehicleById(vehicleId);

        vehicle.withdraw(reason);

        vehicleRepository.save(vehicle);
    }

    @Transactional
    public UUID addExpense(UUID vehicleId, String description, BigDecimal amount, LocalDate date) {
        Vehicle vehicle = getVehicleById(vehicleId);

        Transaction expense = TransactionFactory.createVehicleExpense(vehicle, description, amount, date);
        vehicle.addExpense(expense);

        vehicleRepository.save(vehicle);
        return expense.getId();
    }

    @Transactional
    public UUID generateStandardInspection(UUID vehicleId, LocalDate lastInspection) {
        Vehicle vehicle = getVehicleById(vehicleId);
        Deadline inspectionDeadline;

        if (lastInspection != null) {
            inspectionDeadline = vehicle.generateInspectionFromLastDate(lastInspection);
        } else {
            inspectionDeadline = vehicle.generateStandardInspectionDeadline();
        }

        vehicleRepository.save(vehicle);
        return inspectionDeadline.getId();
    }

    @Transactional
    public UUID addDeadline(UUID vehicleId, DeadlineCreateRequest request) {
        Vehicle vehicle = getVehicleById(vehicleId);

        Deadline newDealine = vehicle.addDeadline(request.reason(), request.dueDate(), request.recurrence(),
                request.recalculateFromCompletion());

        vehicleRepository.save(vehicle);
        return newDealine.getId();
    }

    @Transactional
    public void removeDeadline(UUID vehicleId, UUID deadlineId) {
        Vehicle vehicle = getVehicleById(vehicleId);

        Deadline deadline = deadlineRepository.findById(deadlineId)
                .orElseThrow(() -> new IllegalStateException("Deadline not found of id: " + deadlineId));

        vehicle.removeDeadline(deadline);

        vehicleRepository.save(vehicle);
    }

    @Transactional
    public void updateVehicle(UUID vehicleId, VehicleUpdateRequest request) {
        Vehicle vehicle = getVehicleById(vehicleId);

        if (request.brand() != null) {
            vehicle.setBrand(request.brand());
        }
        if (request.model() != null) {
            vehicle.setModel(request.model());
        }
        if (request.color() != null) {
            vehicle.setColor(request.color());
        }
        if (request.condition() != null) {
            vehicle.setCondition(request.condition());
        }
        if (request.sellingPrice() != null) {
            vehicle.setSellingPrice(request.sellingPrice());
        }
        if (request.handoverDate() != null) {
            vehicle.setHandoverDate(request.handoverDate());
        }
        if (request.licensePlate() != null) {
            vehicle.setLicensePlate(request.licensePlate());
        }
        if (request.registrationDate() != null) {
            vehicle.setRegistrationDate(request.registrationDate());
        }
        if (request.kilometers() != null) {
            vehicle.setKilometers(request.kilometers());
        }
        if (request.inShowroom() != null) {
            vehicle.setIsInShowroom(request.inShowroom());
        }
        if (request.purchaseTransactionAmount() != null && request.purchaseTransactionDate() != null) {
            Transaction transaction = TransactionFactory.createVehiclePurchase(vehicle,
                    request.purchaseTransactionAmount(), request.purchaseTransactionDate());
            vehicle.setPurchaseTransaction(transaction);
        }

        vehicleRepository.save(vehicle);
    }
}