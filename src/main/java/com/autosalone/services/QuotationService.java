package com.autosalone.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.autosalone.dtos.SalesDocumentCreateRequest;
import com.autosalone.dtos.QuotationUpdateRequest;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.enums.VehicleStatus;
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
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found of id: " + id));
    }

    public List<Quotation> getQuotations(LocalDate dateFrom, LocalDate dateTo, Boolean IsArchived, UUID vehicleId,
            UUID customerId, List<QuotationStatus> statusList) {
        return quotationRepository.findQuotations(dateFrom, dateTo, IsArchived, vehicleId, customerId, statusList);
    }

    public List<Quotation> getVisibleQuotationsForCustomer(UUID customerId) {
        return quotationRepository.findVisibleQuotationsByCustomerId(customerId);
    }

    // write

    @Transactional
    public UUID addQuotation(SalesDocumentCreateRequest request) {
        Vehicle vehicle = vehicleService.getVehicleById(request.vehicleId());
        Customer customer = customerService.getCustomerById(request.customerId());

        Quotation newQuotation = new Quotation(vehicle, customer);

        quotationRepository.save(newQuotation);
        return newQuotation.getId();
    }

    @Transactional
    public UUID cloneQuotation(UUID quotationId) {
        Quotation quotation = getQuotationById(quotationId);

        Quotation newQuotation = new Quotation(quotation);

        quotationRepository.save(newQuotation);
        return newQuotation.getId();
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
    public void issueQuotation(UUID quotationId) {
        Quotation quotation = getQuotationById(quotationId);

        quotation.issue();

        Vehicle vehicle = quotation.getVehicle();
        if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
            vehicle.setStatus(VehicleStatus.QUOTED);
            vehicleRepository.save(vehicle);
        }

        quotationRepository.save(quotation);
    }

    @Transactional
    public void archiveQuotation(UUID quotationId) {
        Quotation quotation = getQuotationById(quotationId);

        quotation.archive();

        quotationRepository.save(quotation);
    }

    @Transactional
    public void unarchiveQuotation(UUID quotationId) {
        Quotation quotation = getQuotationById(quotationId);

        quotation.unarchive();

        quotationRepository.save(quotation);
    }

    @Transactional
    public void addItemsToQuotation(UUID quotationId, Set<UUID> catalogItemIds) {
        if (catalogItemIds == null || catalogItemIds.isEmpty())
            return;

        Quotation quotation = getQuotationById(quotationId);

        for (UUID itemId : catalogItemIds) {
            PurchasableItem item = catalogService.getItemById(itemId);
            AppliedItem appliedItem = new AppliedItem(item);
            quotation.addItem(appliedItem);
        }

        quotationRepository.save(quotation);
    }

    @Transactional
    public void updateAppliedItemPrice(UUID quotationId, UUID catalogItemId, BigDecimal newPrice) {
        Quotation quotation = getQuotationById(quotationId);

        AppliedItem targetItem = quotation.getItems().stream()
                .filter(applied -> applied.getItem().getId().equals(catalogItemId))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Accessory of id: " + catalogItemId + " not found in this quotation of id: "
                                        + quotationId));

        quotation.setAppliedItemPrice(targetItem, newPrice);

        quotationRepository.save(quotation);
    }

    @Transactional
    public void removeItemsFromQuotation(UUID quotationId, Set<UUID> catalogItemIds) {
        if (catalogItemIds == null || catalogItemIds.isEmpty())
            return;

        Quotation quotation = getQuotationById(quotationId);

        List<AppliedItem> itemsToRemove = quotation.getItems().stream()
                .filter(applied -> catalogItemIds.contains(applied.getItem().getId()))
                .toList();

        for (AppliedItem item : itemsToRemove) {
            quotation.removeItem(item);
        }

        quotationRepository.save(quotation);
    }

    @Transactional
    public void updateQuotation(UUID quotationId, QuotationUpdateRequest request) {
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
    }
}
