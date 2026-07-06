package com.autosalone.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.autosalone.dtos.SalesDocumentCreateRequest;
import com.autosalone.dtos.ContractUpdateRequest;
import com.autosalone.enums.ContractStatus;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
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
import com.autosalone.repositories.VehicleRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ContractService {

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
                .orElseThrow(() -> new IllegalArgumentException("Contract not found of id: " + id));
    }

    public List<Contract> getContracts(LocalDate dateFrom, LocalDate dateTo, Boolean IsArchived, UUID vehicleId,
            UUID customerId, List<ContractStatus> statusList) {
        return contractRepository.findContracts(dateFrom, dateTo, IsArchived, vehicleId, customerId, statusList);
    }

    public List<Contract> getVisibleContractsForCustomer(UUID customerId) {
        return contractRepository.findVisibleContractsByCustomerId(customerId);
    }

    // write

    @Transactional
    public UUID addContract(SalesDocumentCreateRequest request) {
        Vehicle vehicle = vehicleService.getVehicleById(request.vehicleId());
        Customer customer = customerService.getCustomerById(request.customerId());

        Contract newContract = new Contract(vehicle, customer);

        contractRepository.save(newContract);
        return newContract.getId();
    }

    @Transactional
    public UUID createContractFromQuotation(UUID quotationId) {
        Quotation quotation = quotationService.getQuotationById(quotationId);

        Contract newContract = new Contract(quotation);

        contractRepository.save(newContract);
        return newContract.getId();
    }

    @Transactional
    public void confirmContract(UUID contractId, BigDecimal depositAmount, LocalDate depositDate) {
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
    }

    @Transactional
    public void completeContract(UUID contractId) {
        Contract contract = getContractById(contractId);

        contract.complete();

        Vehicle vehicle = contract.getVehicle();
        vehicle.setIsInShowroom(false);
        if (vehicle.getCondition() == VehicleCondition.NEW)
            vehicle.generateStandardInspectionDeadline();
        vehicle.setStatus(VehicleStatus.SOLD);
        vehicleRepository.save(vehicle);

        contractRepository.save(contract);
    }

    @Transactional
    public void cancelContract(UUID contractId, String reason) {
        Contract contract = getContractById(contractId);

        contract.cancel(reason);

        Vehicle vehicle = contract.getVehicle();
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicleRepository.save(vehicle);

        contractRepository.save(contract);
    }

    @Transactional
    public void archiveContract(UUID contractId) {
        Contract contract = getContractById(contractId);

        contract.archive();

        contractRepository.save(contract);
    }

    @Transactional
    public void unarchiveContract(UUID contractId) {
        Contract contract = getContractById(contractId);

        contract.unarchive();

        contractRepository.save(contract);
    }

    @Transactional
    public UUID addPaymentToContract(UUID contractId, String paymentDescription, BigDecimal paymentAmount,
            LocalDate paymentDate) {
        Contract contract = getContractById(contractId);

        Transaction payment = TransactionFactory.createContractPayment(contract, paymentDescription, paymentAmount,
                paymentDate);
        contract.registerPayment(payment);

        contractRepository.save(contract);
        return payment.getId();
    }

    @Transactional
    public UUID addRefundToContract(UUID contractId, String refundDescription, BigDecimal refundAmount,
            LocalDate refundDate) {
        Contract contract = getContractById(contractId);

        Transaction refund = TransactionFactory.createContractPayment(contract, refundDescription, refundAmount,
                refundDate);
        contract.registerRefund(refund);

        contractRepository.save(contract);
        return refund.getId();
    }

    @Transactional
    public void addItemsToContract(UUID contractId, Set<UUID> catalogItemIds) {
        if (catalogItemIds == null || catalogItemIds.isEmpty())
            return;

        Contract contract = getContractById(contractId);

        for (UUID itemId : catalogItemIds) {
            PurchasableItem item = catalogService.getItemById(itemId);
            AppliedItem appliedItem = new AppliedItem(item);
            contract.addItem(appliedItem);
        }

        contractRepository.save(contract);
    }

    @Transactional
    public void updateAppliedItemPrice(UUID contractId, UUID catalogItemId, BigDecimal newPrice) {
        Contract contract = getContractById(contractId);

        AppliedItem targetItem = contract.getItems().stream()
                .filter(applied -> applied.getItem().getId().equals(catalogItemId))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Accessory of id: " + catalogItemId + " not found in this contract of id: "
                                        + contractId));

        contract.setAppliedItemPrice(targetItem, newPrice);

        contractRepository.save(contract);
    }

    @Transactional
    public void removeItemsFromContract(UUID contractId, Set<UUID> catalogItemIds) {
        if (catalogItemIds == null || catalogItemIds.isEmpty())
            return;

        Contract contract = getContractById(contractId);

        List<AppliedItem> itemsToRemove = contract.getItems().stream()
                .filter(applied -> catalogItemIds.contains(applied.getItem().getId()))
                .toList();

        for (AppliedItem item : itemsToRemove) {
            contract.removeItem(item);
        }

        contractRepository.save(contract);
    }

    @Transactional
    public void updateContract(UUID quotationId, ContractUpdateRequest request) {
        Contract contract = getContractById(quotationId);

        if (request.estimatedHandoverDate() != null) {
            contract.setEstimatedHandoverDate(request.estimatedHandoverDate());
        }
        if (request.date() != null) {
            contract.setDate(request.date());
        }
        if (request.vehicleId() != null) {
            Vehicle vehicle = vehicleService.getVehicleById(request.vehicleId());
            contract.setVehicle(vehicle);
        }
        if (request.customerId() != null) {
            Customer customer = customerService.getCustomerById(request.customerId());
            contract.setCustomer(customer);
        }
        if (request.additionalFees() != null) {
            contract.setAdditionalFees(request.additionalFees());
        }
        if (request.publicNotes() != null) {
            contract.setPublicNotes(request.publicNotes());
        }
        if (request.internalNotes() != null) {
            contract.setInternalNotes(request.internalNotes());
        }
        if (request.vehicleSellingPrice() != null) {
            contract.setVehicleSellingPriceSnapshot(request.vehicleSellingPrice());
        }
        if (request.discountType() != null) {
            DiscountStrategy newStrategy = request.discountType().createStrategy(request.discountValue());
            contract.setDiscountStrategy(newStrategy);
        }
    }
}
