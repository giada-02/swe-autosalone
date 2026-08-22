package com.autosalone.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.autosalone.dtos.requests.ContractUpdateRequest;
import com.autosalone.dtos.requests.SalesDocumentCreateRequest;
import com.autosalone.dtos.responses.ContractResponse;
import com.autosalone.dtos.responses.TransactionResponse;
import com.autosalone.enums.ContractStatus;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Customer;
import com.autosalone.models.Quotation;
import com.autosalone.models.Transaction;
import com.autosalone.models.TransactionFactory;
import com.autosalone.models.Contract;
import com.autosalone.models.Vehicle;
import com.autosalone.models.catalog.AppliedItem;
import com.autosalone.models.catalog.PurchasableItem;
import com.autosalone.models.discounts.DiscountStrategy;
import com.autosalone.repositories.ContractRepository;
import com.autosalone.repositories.QuotationRepository;
import com.autosalone.repositories.TransactionRepository;
import com.autosalone.repositories.VehicleRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ContractService {

    @Inject
    private TransactionRepository transactionRepository;

    @Inject
    private ContractRepository contractRepository;

    @Inject
    private QuotationRepository quotationRepository;
    @Inject
    private QuotationService quotationService;

    @Inject
    private VehicleRepository vehicleRepository;
    @Inject
    private VehicleService vehicleService;

    @Inject
    private CustomerService customerService;

    @Inject
    private CatalogService catalogService;

    // read

    public Contract getContractById(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found of id: " + id));
    }

    public ContractResponse getContractResponseById(UUID id) {
        Contract contract = getContractById(id);
        return ContractResponse.fromEntity(contract);
    }

    public List<ContractResponse> getContracts(LocalDate dateFrom, LocalDate dateTo, Boolean IsArchived, UUID vehicleId,
            UUID customerId, List<ContractStatus> statusList) {
        return contractRepository.findContracts(dateFrom, dateTo, IsArchived, vehicleId, customerId, statusList)
                .stream().map(ContractResponse::fromEntity).toList();
    }

    public List<ContractResponse> getVisibleContractsForCustomer(UUID customerId) {
        return contractRepository.findVisibleContractsByCustomerId(customerId)
                .stream().map(ContractResponse::fromEntity).toList();
    }

    // write

    @Transactional
    public ContractResponse addContract(SalesDocumentCreateRequest request) {
        Vehicle vehicle = vehicleService.getVehicleById(request.vehicleId());
        Customer customer = customerService.getCustomerById(request.customerId());

        Contract contract = new Contract(vehicle, customer);

        contractRepository.save(contract);
        return ContractResponse.fromEntity(contract);
    }

    @Transactional
    public ContractResponse createContractFromQuotation(UUID quotationId) {
        Quotation quotation = quotationService.getQuotationById(quotationId);

        Contract contract = new Contract(quotation);

        contractRepository.save(contract);
        return ContractResponse.fromEntity(contract);
    }

    @Transactional
    public ContractResponse confirmContract(UUID contractId, BigDecimal depositAmount, LocalDate depositDate) {
        Contract contract = getContractById(contractId);

        Transaction deposit = null;
        if (depositAmount != null && depositDate != null)
            deposit = TransactionFactory.createContractDeposit(contract, depositAmount, depositDate);

        contract.confirm(deposit);

        Quotation quotation = contract.getQuotationReference();
        if (quotation != null) {
            quotation.accept();
            quotationRepository.save(quotation);
        }

        Vehicle vehicle = contract.getVehicle();
        vehicle.setStatus(VehicleStatus.RESERVED);
        vehicleRepository.save(vehicle);

        UUID quotationIdToExclude = quotation != null ? quotation.getId() : null;
        List<Quotation> quotations = quotationRepository
                .findConflictingQuotationsForVehicle(vehicle.getId(), quotationIdToExclude);
        List<Contract> contracts = contractRepository.findConflictingContractsForVehicle(vehicle.getId(),
                contractId);

        for (Quotation q : quotations) {
            q.voidDocument();
            quotationRepository.save(q);
        }

        for (Contract c : contracts) {
            c.voidDocument();
            contractRepository.save(c);
        }

        contractRepository.save(contract);
        return ContractResponse.fromEntity(contract);
    }

    @Transactional
    public ContractResponse completeContract(UUID contractId) {
        Contract contract = getContractById(contractId);

        contract.complete();

        Vehicle vehicle = contract.getVehicle();
        vehicle.setIsInShowroom(false);
        if (vehicle.getCondition() == VehicleCondition.NEW)
            vehicle.generateStandardInspectionDeadline();
        vehicle.setStatus(VehicleStatus.SOLD);
        vehicleRepository.save(vehicle);

        contractRepository.save(contract);
        return ContractResponse.fromEntity(contract);
    }

    @Transactional
    public ContractResponse cancelContract(UUID contractId, String reason) {
        Contract contract = getContractById(contractId);

        contract.cancel(reason);

        Vehicle vehicle = contract.getVehicle();
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicleRepository.save(vehicle);

        contractRepository.save(contract);
        return ContractResponse.fromEntity(contract);
    }

    @Transactional
    public ContractResponse archiveContract(UUID contractId) {
        Contract contract = getContractById(contractId);

        contract.archive();

        contractRepository.save(contract);
        return ContractResponse.fromEntity(contract);
    }

    @Transactional
    public ContractResponse unarchiveContract(UUID contractId) {
        Contract contract = getContractById(contractId);

        contract.unarchive();

        contractRepository.save(contract);
        return ContractResponse.fromEntity(contract);
    }

    // payments

    @Transactional
    public TransactionResponse addPaymentToContract(UUID contractId, String paymentDescription,
            BigDecimal paymentAmount,
            LocalDate paymentDate) {
        Contract contract = getContractById(contractId);

        Transaction payment = TransactionFactory.createContractPayment(contract, paymentDescription, paymentAmount,
                paymentDate);
        contract.registerPayment(payment);

        transactionRepository.save(payment);
        return TransactionResponse.fromEntity(payment);
    }

    @Transactional
    public TransactionResponse addRefundToContract(UUID contractId, String refundDescription, BigDecimal refundAmount,
            LocalDate refundDate) {
        Contract contract = getContractById(contractId);

        Transaction refund = TransactionFactory.createContractRefund(contract, refundDescription, refundAmount,
                refundDate);
        contract.registerRefund(refund);

        transactionRepository.save(refund);
        return TransactionResponse.fromEntity(refund);
    }

    @Transactional
    public ContractResponse addItemsToContract(UUID contractId, Set<UUID> catalogItemIds) {
        Contract contract = getContractById(contractId);

        if (catalogItemIds == null || catalogItemIds.isEmpty())
            return ContractResponse.fromEntity(contract);

        for (UUID itemId : catalogItemIds) {
            PurchasableItem item = catalogService.getItemById(itemId);
            AppliedItem appliedItem = new AppliedItem(item);
            contract.addItem(appliedItem);
        }

        contractRepository.save(contract);
        return ContractResponse.fromEntity(contract);

    }

    @Transactional
    public ContractResponse updateAppliedItemPrice(UUID contractId, UUID catalogItemId, BigDecimal newPrice) {
        Contract contract = getContractById(contractId);

        AppliedItem targetItem = contract.getItems().stream()
                .filter(applied -> applied.getItem().getId().equals(catalogItemId))
                .findFirst()
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Accessory of id: " + catalogItemId + " not found in this contract of id: "
                                        + contractId));

        contract.setAppliedItemPrice(targetItem, newPrice);

        contractRepository.save(contract);
        return ContractResponse.fromEntity(contract);
    }

    @Transactional
    public ContractResponse removeItemsFromContract(UUID contractId, Set<UUID> catalogItemIds) {
        Contract contract = getContractById(contractId);

        if (catalogItemIds == null || catalogItemIds.isEmpty())
            return ContractResponse.fromEntity(contract);

        List<AppliedItem> itemsToRemove = contract.getItems().stream()
                .filter(applied -> catalogItemIds.contains(applied.getItem().getId()))
                .toList();

        for (AppliedItem item : itemsToRemove) {
            contract.removeItem(item);
        }

        contractRepository.save(contract);
        return ContractResponse.fromEntity(contract);
    }

    @Transactional
    public ContractResponse updateContract(UUID contractId, ContractUpdateRequest request) {
        Contract contract = getContractById(contractId);

        contract.setEstimatedHandoverDate(request.estimatedHandoverDate());
        contract.setDate(request.date());
        contract.setAdditionalFees(request.additionalFees());
        contract.setPublicNotes(request.publicNotes());
        contract.setInternalNotes(request.internalNotes());
        contract.setVehicleSellingPriceSnapshot(request.vehicleSellingPrice());

        if (!contract.getVehicle().getId().equals(request.vehicleId())) {
            Vehicle vehicle = vehicleService.getVehicleById(request.vehicleId());
            contract.setVehicle(vehicle);
        }

        if (!contract.getCustomer().getId().equals(request.customerId())) {
            Customer customer = customerService.getCustomerById(request.customerId());
            contract.setCustomer(customer);
        }

        if (request.discountType() != null) {
            DiscountStrategy newStrategy = request.discountType().createStrategy(request.discountValue());
            contract.setDiscountStrategy(newStrategy);
        } else {
            contract.setDiscountStrategy(null);
        }

        contractRepository.save(contract);
        return ContractResponse.fromEntity(contract);
    }
}
