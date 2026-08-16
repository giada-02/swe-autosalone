package com.autosalone.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.requests.DeadlineRequest;
import com.autosalone.dtos.requests.PurchaseTransactionRequest;
import com.autosalone.dtos.requests.VehicleCreateRequest;
import com.autosalone.dtos.requests.VehicleUpdateRequest;
import com.autosalone.dtos.responses.DeadlineResponse;
import com.autosalone.dtos.responses.ExpenseResponse;
import com.autosalone.dtos.responses.VehicleResponse;
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
import com.autosalone.repositories.TransactionRepository;
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
    private TransactionRepository transactionRepository;

    @Inject
    private QuotationRepository quotationRepository;

    @Inject
    private ContractRepository contractRepository;

    // read

    public Vehicle getVehicleById(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found of id: " + id));
    }

    public VehicleResponse getVehicleResponseById(UUID id) {
        Vehicle vehicle = getVehicleById(id);
        return VehicleResponse.fromEntity(vehicle);
    }

    public List<VehicleResponse> getVehicles(String keyword, String brand, VehicleCondition condition,
            BigDecimal maxPrice, Boolean isInShowroom, List<VehicleStatus> statusList) {
        String sanitizedKeyword = Utils.sanitizeLikeKeyword(keyword);
        return vehicleRepository.findVehicles(sanitizedKeyword, brand, condition, maxPrice, isInShowroom, statusList)
                .stream().map(VehicleResponse::fromEntity).toList();
    }

    public List<String> getAllBrands() {
        return vehicleRepository.findAllBrands();
    }

    // write

    @Transactional
    public VehicleResponse addVehicle(VehicleCreateRequest request) {

        Vehicle vehicle = new Vehicle.VehicleBuilder()
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

        if (request.purchaseTransaction() != null) {
            Transaction purchase = TransactionFactory.createVehiclePurchase(vehicle,
                    request.purchaseTransaction().amount(), request.purchaseTransaction().date());
            vehicle.setPurchaseTransaction(purchase);
        }

        vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(vehicle);
    }

    @Transactional
    public VehicleResponse addPurchaseTransaction(UUID vehicleId, PurchaseTransactionRequest request) {
        Vehicle vehicle = getVehicleById(vehicleId);

        if (vehicle.getPurchaseTransaction() != null)
            throw new IllegalStateException("A purchase transaction already exists for this vehicle");

        Transaction purchase = TransactionFactory.createVehiclePurchase(vehicle,
                request.amount(), request.date());
        vehicle.setPurchaseTransaction(purchase);

        vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(vehicle);
    }

    @Transactional
    public VehicleResponse withdrawVehicle(UUID vehicleId, String reason) {
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

        List<Deadline> pendingDeadlines = deadlineRepository.findPendingByVehicleId(vehicleId);
        for (Deadline deadline : pendingDeadlines) {
            deadlineRepository.delete(deadline);
        }

        return VehicleResponse.fromEntity(vehicle);
    }

    @Transactional
    public ExpenseResponse addExpense(UUID vehicleId, String description, BigDecimal amount, LocalDate date) {
        Vehicle vehicle = getVehicleById(vehicleId);
        String sanitizedDescription = Utils.sanitizeText(description);
        Transaction expense = TransactionFactory.createVehicleExpense(vehicle, sanitizedDescription, amount, date);
        vehicle.addExpense(expense);

        transactionRepository.save(expense);
        return ExpenseResponse.fromEntity(expense);
    }

    @Transactional
    public DeadlineResponse generateStandardInspection(UUID vehicleId, LocalDate lastInspection) {
        Vehicle vehicle = getVehicleById(vehicleId);
        Deadline inspectionDeadline;

        if (lastInspection != null) {
            inspectionDeadline = vehicle.generateInspectionFromLastDate(lastInspection);
        } else {
            inspectionDeadline = vehicle.generateStandardInspectionDeadline();
        }

        deadlineRepository.save(inspectionDeadline);
        return DeadlineResponse.fromEntity(inspectionDeadline);
    }

    @Transactional
    public DeadlineResponse addDeadline(UUID vehicleId, DeadlineRequest request) {
        Vehicle vehicle = getVehicleById(vehicleId);

        Deadline deadline = vehicle.addDeadline(request.reason(), request.dueDate(), request.recurrence(),
                request.recalculateFromCompletion());

        deadlineRepository.save(deadline);
        return DeadlineResponse.fromEntity(deadline);
    }

    @Transactional
    public void removeDeadline(UUID vehicleId, UUID deadlineId) {
        Vehicle vehicle = getVehicleById(vehicleId);

        Deadline deadline = deadlineRepository.findById(deadlineId)
                .orElseThrow(() -> new ResourceNotFoundException("Deadline not found of id: " + deadlineId));

        vehicle.removeDeadline(deadline);
        deadlineRepository.save(deadline);
    }

    @Transactional
    public VehicleResponse updateVehicle(UUID vehicleId, VehicleUpdateRequest request) {
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

        vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(vehicle);
    }
}