package com.autosalone.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.autosalone.dtos.requests.QuotationUpdateRequest;
import com.autosalone.dtos.requests.SalesDocumentCreateRequest;
import com.autosalone.dtos.responses.QuotationCleanupResponse;
import com.autosalone.dtos.responses.QuotationCustomerResponse;
import com.autosalone.dtos.responses.QuotationResponse;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Contract;
import com.autosalone.models.Customer;
import com.autosalone.models.Quotation;
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
public class QuotationService {

    @Inject
    private QuotationRepository quotationRepository;

    @Inject
    private ContractRepository contractRepository;

    @Inject
    private VehicleRepository vehicleRepository;
    @Inject
    private VehicleService vehicleService;

    @Inject
    private CustomerService customerService;

    @Inject
    private CatalogService catalogService;

    @Inject
    private Clock clock;

    // read

    public Quotation getQuotationById(UUID id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found of id: " + id));
    }

    public QuotationResponse getQuotationResponseById(UUID id) {
        LocalDate today = LocalDate.now(clock);
        Quotation quotation = getQuotationById(id);
        return QuotationResponse.fromEntity(quotation, today);
    }

    public QuotationCustomerResponse getQuotationCustomerResponseById(UUID id) {
        LocalDate today = LocalDate.now(clock);
        Quotation quotation = getQuotationById(id);
        return QuotationCustomerResponse.fromEntity(quotation, today);
    }

    public List<QuotationResponse> getQuotationsForOwner(LocalDate dateFrom, LocalDate dateTo, Boolean isArchived,
            UUID vehicleId, UUID customerId, List<QuotationStatus> statusList) {
        LocalDate today = LocalDate.now(clock);
        return quotationRepository
                .findQuotations(dateFrom, dateTo, isArchived, vehicleId, customerId, statusList, today)
                .stream().map((quotation) -> QuotationResponse.fromEntity(quotation, today)).toList();
    }

    public List<QuotationCustomerResponse> getQuotationsForCustomer(LocalDate dateFrom, LocalDate dateTo,
            Boolean isArchived, UUID vehicleId, UUID customerId, List<QuotationStatus> statusList) {
        LocalDate today = LocalDate.now(clock);
        return quotationRepository
                .findQuotations(dateFrom, dateTo, isArchived, vehicleId, customerId, statusList, today)
                .stream().map((quotation) -> QuotationCustomerResponse.fromEntity(quotation, today))
                .toList();
    }

    public List<QuotationResponse> getVisibleQuotationsForCustomer(UUID customerId) {
        LocalDate today = LocalDate.now(clock);
        return quotationRepository.findVisibleQuotationsByCustomerId(customerId)
                .stream().map((quotation) -> QuotationResponse.fromEntity(quotation, today)).toList();
    }

    // write

    @Transactional
    public QuotationResponse addQuotation(SalesDocumentCreateRequest request) {
        LocalDate today = LocalDate.now(clock);
        Vehicle vehicle = vehicleService.getVehicleById(request.vehicleId());
        Customer customer = customerService.getCustomerById(request.customerId());

        Quotation quotation = new Quotation(vehicle, customer, today);

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation, today);
    }

    @Transactional
    public QuotationResponse cloneQuotation(UUID quotationId) {
        LocalDate today = LocalDate.now(clock);
        Quotation quotation = getQuotationById(quotationId);

        Quotation clonedQuotation = new Quotation(quotation, today);

        quotationRepository.save(clonedQuotation);
        return QuotationResponse.fromEntity(clonedQuotation, today);
    }

    @Transactional
    public QuotationCleanupResponse expireOutdatedQuotations() {
        LocalDate today = LocalDate.now(clock);
        List<Quotation> expiredQuotations = quotationRepository.findExpiredQuotations(today);

        Set<Vehicle> affectedVehicles = new HashSet<>();
        int voidedContractsCount = 0;
        int freedVehiclesCount = 0;

        for (Quotation quotation : expiredQuotations) {
            quotation.expire();
            quotationRepository.save(quotation);

            List<Contract> linkedDrafts = contractRepository.findDraftContractsBySourceQuotation(quotation.getId());
            for (Contract draftContract : linkedDrafts) {
                draftContract.voidDocument();
                contractRepository.save(draftContract);
                voidedContractsCount++;
            }

            affectedVehicles.add(quotation.getVehicle());
        }

        for (Vehicle vehicle : affectedVehicles) {
            if (vehicle.getStatus() == VehicleStatus.QUOTED
                    && !quotationRepository.existsIssuedQuotationForVehicle(vehicle.getId())) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(vehicle);
                freedVehiclesCount++;
            }
        }

        return new QuotationCleanupResponse(
                expiredQuotations.size(),
                voidedContractsCount,
                freedVehiclesCount);
    }

    @Transactional
    public QuotationResponse issueQuotation(UUID quotationId) {
        LocalDate today = LocalDate.now(clock);
        Quotation quotation = getQuotationById(quotationId);

        quotation.issue(today);

        Vehicle vehicle = quotation.getVehicle();
        if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
            vehicle.setStatus(VehicleStatus.QUOTED);
            vehicleRepository.save(vehicle);
        }

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation, today);
    }

    @Transactional
    public QuotationResponse archiveQuotation(UUID quotationId) {
        LocalDate today = LocalDate.now(clock);
        Quotation quotation = getQuotationById(quotationId);

        quotation.archive(today);

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation, today);
    }

    @Transactional
    public QuotationResponse unarchiveQuotation(UUID quotationId) {
        LocalDate today = LocalDate.now(clock);
        Quotation quotation = getQuotationById(quotationId);

        quotation.unarchive();

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation, today);
    }

    @Transactional
    public QuotationResponse addItemsToQuotation(UUID quotationId, Set<UUID> catalogItemIds) {
        LocalDate today = LocalDate.now(clock);
        Quotation quotation = getQuotationById(quotationId);

        if (catalogItemIds == null || catalogItemIds.isEmpty())
            return QuotationResponse.fromEntity(quotation, today);

        for (UUID itemId : catalogItemIds) {
            PurchasableItem item = catalogService.getItemById(itemId);
            AppliedItem appliedItem = new AppliedItem(item);
            quotation.addItem(appliedItem);
        }

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation, today);
    }

    @Transactional
    public QuotationResponse updateAppliedItemPrice(UUID quotationId, UUID catalogItemId, BigDecimal newPrice) {
        LocalDate today = LocalDate.now(clock);
        Quotation quotation = getQuotationById(quotationId);

        AppliedItem targetItem = quotation.getItems().stream()
                .filter(applied -> Objects.equals(applied.getItem().getId(), catalogItemId))
                .findFirst()
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Accessory of id: " + catalogItemId + " not found in this quotation of id: "
                                        + quotationId));

        quotation.setAppliedItemPrice(targetItem, newPrice);

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation, today);
    }

    @Transactional
    public QuotationResponse removeItemsFromQuotation(UUID quotationId, Set<UUID> catalogItemIds) {
        LocalDate today = LocalDate.now(clock);
        Quotation quotation = getQuotationById(quotationId);

        if (catalogItemIds == null || catalogItemIds.isEmpty())
            return QuotationResponse.fromEntity(quotation, today);

        List<AppliedItem> itemsToRemove = quotation.getItems().stream()
                .filter(applied -> catalogItemIds.contains(applied.getItem().getId()))
                .toList();

        for (AppliedItem item : itemsToRemove) {
            quotation.removeItem(item);
        }

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation, today);
    }

    @Transactional
    public QuotationResponse updateQuotation(UUID quotationId, QuotationUpdateRequest request) {
        LocalDate today = LocalDate.now(clock);
        Quotation quotation = getQuotationById(quotationId);

        quotation.updateExpiration(request.expirationPolicy(), request.expirationDate());
        quotation.setDate(request.date());
        quotation.setAdditionalFees(request.additionalFees());
        quotation.setPublicNotes(request.publicNotes());
        quotation.setInternalNotes(request.internalNotes());
        quotation.setVehicleSellingPriceSnapshot(request.vehicleSellingPrice());

        if (!quotation.getVehicle().getId().equals(request.vehicleId())) {
            Vehicle vehicle = vehicleService.getVehicleById(request.vehicleId());
            quotation.setVehicle(vehicle);
        }

        if (!quotation.getCustomer().getId().equals(request.customerId())) {
            Customer customer = customerService.getCustomerById(request.customerId());
            quotation.setCustomer(customer);
        }

        if (request.discountType() != null) {
            DiscountStrategy newStrategy = request.discountType().createStrategy(request.discountValue());
            quotation.setDiscountStrategy(newStrategy);
        } else {
            quotation.setDiscountStrategy(null);
        }

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation, today);
    }
}
