package com.autosalone.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.autosalone.dtos.requests.QuotationUpdateRequest;
import com.autosalone.dtos.requests.SalesDocumentCreateRequest;
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

    // read

    public Quotation getQuotationById(UUID id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found of id: " + id));
    }

    public QuotationResponse getQuotationResponseById(UUID id) {
        Quotation quotation = getQuotationById(id);
        return QuotationResponse.fromEntity(quotation);
    }

    public List<QuotationResponse> getQuotations(LocalDate dateFrom, LocalDate dateTo, Boolean IsArchived,
            UUID vehicleId,
            UUID customerId, List<QuotationStatus> statusList) {
        return quotationRepository.findQuotations(dateFrom, dateTo, IsArchived, vehicleId, customerId, statusList)
                .stream().map(QuotationResponse::fromEntity).toList();
    }

    public List<QuotationResponse> getVisibleQuotationsForCustomer(UUID customerId) {
        return quotationRepository.findVisibleQuotationsByCustomerId(customerId)
                .stream().map(QuotationResponse::fromEntity).toList();
    }

    // write

    @Transactional
    public QuotationResponse addQuotation(SalesDocumentCreateRequest request) {
        Vehicle vehicle = vehicleService.getVehicleById(request.vehicleId());
        Customer customer = customerService.getCustomerById(request.customerId());

        Quotation quotation = new Quotation(vehicle, customer);

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation);
    }

    @Transactional
    public QuotationResponse cloneQuotation(UUID quotationId) {
        Quotation quotation = getQuotationById(quotationId);

        Quotation clonedQuotation = new Quotation(quotation);

        quotationRepository.save(clonedQuotation);
        return QuotationResponse.fromEntity(clonedQuotation);
    }

    @Transactional
    public void expireOutdatedQuotations() {
        LocalDate today = LocalDate.now();
        List<Quotation> expiredQuotations = quotationRepository.findExpiredQuotations(today);

        Set<Vehicle> affectedVehicles = new HashSet<>();

        for (Quotation quotation : expiredQuotations) {
            quotation.expire();
            quotationRepository.save(quotation);

            List<Contract> linkedDrafts = contractRepository.findDraftContractsBySourceQuotation(quotation.getId());
            for (Contract draftContract : linkedDrafts) {
                draftContract.voidDocument();
                contractRepository.save(draftContract);
            }

            affectedVehicles.add(quotation.getVehicle());
        }

        for (Vehicle vehicle : affectedVehicles) {
            boolean hasOtherActiveDocuments = !quotationRepository
                    .findConflictingQuotationsForVehicle(vehicle.getId(), null).isEmpty()
                    || !contractRepository.findConflictingContractsForVehicle(vehicle.getId(), null).isEmpty();

            if (!hasOtherActiveDocuments) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(vehicle);
            }
        }
    }

    @Transactional
    public QuotationResponse issueQuotation(UUID quotationId) {
        Quotation quotation = getQuotationById(quotationId);

        quotation.issue();

        Vehicle vehicle = quotation.getVehicle();
        if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
            vehicle.setStatus(VehicleStatus.QUOTED);
            vehicleRepository.save(vehicle);
        }

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation);
    }

    @Transactional
    public QuotationResponse archiveQuotation(UUID quotationId) {
        Quotation quotation = getQuotationById(quotationId);

        quotation.archive();

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation);
    }

    @Transactional
    public QuotationResponse unarchiveQuotation(UUID quotationId) {
        Quotation quotation = getQuotationById(quotationId);

        quotation.unarchive();

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation);
    }

    @Transactional
    public QuotationResponse addItemsToQuotation(UUID quotationId, Set<UUID> catalogItemIds) {
        Quotation quotation = getQuotationById(quotationId);

        if (catalogItemIds == null || catalogItemIds.isEmpty())
            return QuotationResponse.fromEntity(quotation);

        for (UUID itemId : catalogItemIds) {
            PurchasableItem item = catalogService.getItemById(itemId);
            AppliedItem appliedItem = new AppliedItem(item);
            quotation.addItem(appliedItem);
        }

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation);
    }

    @Transactional
    public QuotationResponse updateAppliedItemPrice(UUID quotationId, UUID catalogItemId, BigDecimal newPrice) {
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
        return QuotationResponse.fromEntity(quotation);
    }

    @Transactional
    public QuotationResponse removeItemsFromQuotation(UUID quotationId, Set<UUID> catalogItemIds) {
        Quotation quotation = getQuotationById(quotationId);

        if (catalogItemIds == null || catalogItemIds.isEmpty())
            return QuotationResponse.fromEntity(quotation);

        List<AppliedItem> itemsToRemove = quotation.getItems().stream()
                .filter(applied -> catalogItemIds.contains(applied.getItem().getId()))
                .toList();

        for (AppliedItem item : itemsToRemove) {
            quotation.removeItem(item);
        }

        quotationRepository.save(quotation);
        return QuotationResponse.fromEntity(quotation);
    }

    @Transactional
    public QuotationResponse updateQuotation(UUID quotationId, QuotationUpdateRequest request) {
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
        return QuotationResponse.fromEntity(quotation);
    }
}
