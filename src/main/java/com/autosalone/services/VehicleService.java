package com.autosalone.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.DeadlineRequest;
import com.autosalone.dtos.VehicleRequest;
import com.autosalone.enums.ContractStatus;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Contract;
import com.autosalone.models.Deadline;
import com.autosalone.models.Quotation;
import com.autosalone.models.Transaction;
import com.autosalone.models.TransactionFactory;
import com.autosalone.models.Vehicle;
import com.autosalone.repositories.ContractRepository;
import com.autosalone.repositories.DeadlineRepository;
import com.autosalone.repositories.QuotationRepository;
import com.autosalone.repositories.VehicleRepository;
import com.autosalone.utils.Utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class VehicleService {

    @Inject
    private VehicleRepository vehicleRepository;

    @Inject
    private DeadlineRepository deadlineRepository;

    @Inject
    private QuotationRepository quotationRepository;

    @Inject
    private ContractRepository contractRepository;

    // read

    public Vehicle getVehicleById(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found of id: " + id));
    }

    public List<Vehicle> getVehicles(String keyword, String brand, VehicleCondition condition,
            BigDecimal maxPrice, Boolean isInShowroom, List<VehicleStatus> statusList) {
        String sanitizedKeyword = Utils.sanitizeLikeKeyword(keyword);
        return vehicleRepository.findVehicles(sanitizedKeyword, brand, condition, maxPrice, isInShowroom, statusList);
    }

    public List<String> getAllBrands() {
        return vehicleRepository.findAllBrands();
    }

    // write

    @Transactional
    public UUID addVehicle(VehicleRequest request) {

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

        List<Quotation> openQuotations = quotationRepository.findQuotations(
                null, null, false, vehicleId, null, List.of(
                        QuotationStatus.DRAFT,
                        QuotationStatus.ISSUED));

        for (Quotation quotation : openQuotations) {
            quotation.voidDocument();
            quotationRepository.save(quotation);
        }

        List<Contract> activeContracts = contractRepository.findContracts(
                null, null, false, vehicleId, null, List.of(
                        ContractStatus.DRAFT,
                        ContractStatus.CONFIRMED));

        for (Contract contract : activeContracts) {
            if (contract.getStatus() == ContractStatus.DRAFT) {
                contract.voidDocument();
            } else {
                contract.cancel(reason);
            }
            contractRepository.save(contract);
        }
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
    public UUID addDeadline(UUID vehicleId, DeadlineRequest request) {
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
                .orElseThrow(() -> new ResourceNotFoundException("Deadline not found of id: " + deadlineId));

        vehicle.removeDeadline(deadline);

        vehicleRepository.save(vehicle);
    }

    @Transactional
    public void updateVehicle(UUID vehicleId, VehicleRequest request) {
        Vehicle vehicle = getVehicleById(vehicleId);

        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setColor(request.color());
        vehicle.setCondition(request.condition());
        vehicle.setSellingPrice(request.sellingPrice());
        vehicle.setHandoverDate(request.handoverDate());
        vehicle.setLicensePlate(request.licensePlate());
        vehicle.setRegistrationDate(request.registrationDate());
        vehicle.setKilometers(request.kilometers());
        vehicle.setIsInShowroom(request.inShowroom());

        if (request.purchaseTransactionAmount() != null && request.purchaseTransactionDate() != null
                && vehicle.getPurchaseTransaction() == null) {
            Transaction transaction = TransactionFactory.createVehiclePurchase(vehicle,
                    request.purchaseTransactionAmount(), request.purchaseTransactionDate());
            vehicle.setPurchaseTransaction(transaction);
        }

        vehicleRepository.save(vehicle);
    }
}