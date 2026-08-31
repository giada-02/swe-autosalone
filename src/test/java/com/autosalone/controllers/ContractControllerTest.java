package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.requests.CatalogItemIdsRequest;
import com.autosalone.dtos.requests.CatalogItemPriceUpdateRequest;
import com.autosalone.dtos.requests.ContractCancelRequest;
import com.autosalone.dtos.requests.ContractConfirmRequest;
import com.autosalone.dtos.requests.ContractUpdateRequest;
import com.autosalone.dtos.requests.PaymentRecordRequest;
import com.autosalone.dtos.requests.SalesDocumentCreateRequest;
import com.autosalone.dtos.responses.ContractCustomerResponse;
import com.autosalone.dtos.responses.ContractResponse;
import com.autosalone.dtos.responses.CustomerResponse;
import com.autosalone.dtos.responses.TransactionResponse;
import com.autosalone.enums.ContractStatus;
import com.autosalone.enums.TransactionType;
import com.autosalone.services.ContractService;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@ExtendWith(MockitoExtension.class)
class ContractControllerTest {

    @Mock
    private ContractService contractService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Principal principal;

    @InjectMocks
    private ContractController contractController;

    private UUID contractId;
    private UUID quotationId;
    private UUID vehicleId;
    private UUID customerId;
    private UUID transactionId;
    private UUID itemId;
    private LocalDate now;

    private ContractResponse contractResponse;
    private ContractCustomerResponse contractCustomerResponse;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();
        contractId = UUID.randomUUID();
        quotationId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        contractResponse = buildContractResponse(contractId);
        contractCustomerResponse = buildContractCustomerResponse(contractId);
        transactionResponse = buildTransactionResponse(transactionId);
    }

    @Test
    void getContracts_AsOwner_Returns200AndList() {
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(false);
        List<ContractStatus> statusList = List.of(ContractStatus.DRAFT, ContractStatus.CONFIRMED);
        when(contractService.getContractsForOwner(now, now, false, vehicleId, customerId, statusList))
                .thenReturn(List.of(contractResponse));

        Response response = contractController.getContracts(now.toString(), now.toString(), false, vehicleId,
                customerId, statusList);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(contractService).getContractsForOwner(now, now, false, vehicleId, customerId, statusList);
    }

    @Test
    void getContracts_AsCustomer_EmptyStatusList_UsesDefaults() {
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());

        List<ContractStatus> statusList = List.of(ContractStatus.CONFIRMED, ContractStatus.COMPLETED,
                ContractStatus.CANCELED, ContractStatus.VOIDED);

        when(contractService.getContractsForCustomer(now, now, false, vehicleId, customerId, statusList))
                .thenReturn(List.of(contractCustomerResponse));

        Response response = contractController.getContracts(now.toString(), now.toString(), false, vehicleId, null,
                List.of());

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(contractService).getContractsForCustomer(now, now, false, vehicleId, customerId, statusList);
    }

    @Test
    void getContracts_AsCustomer_RemovesDraftFromStatusList() {
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());

        List<ContractStatus> statusList = List.of(ContractStatus.DRAFT, ContractStatus.CONFIRMED);
        List<ContractStatus> expectedStatusList = List.of(ContractStatus.CONFIRMED);

        when(contractService.getContractsForCustomer(now, now, false, vehicleId, customerId, expectedStatusList))
                .thenReturn(List.of(contractCustomerResponse));

        Response response = contractController.getContracts(now.toString(), now.toString(), false, vehicleId, null,
                statusList);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(contractService).getContractsForCustomer(now, now, false, vehicleId, customerId, expectedStatusList);
    }

    @Test
    void getContractById_AsOwner_Returns200AndContract() {
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(false);
        when(contractService.getContractResponseById(contractId)).thenReturn(contractResponse);

        Response response = contractController.getContractById(contractId);

        assertEquals(200, response.getStatus());
        assertEquals(contractResponse, response.getEntity());
        verify(contractService).getContractResponseById(contractId);
    }

    @Test
    void getContractById_AsCustomer_Returns200AndContract() {
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());

        CustomerResponse customerResponse = new CustomerResponse(customerId, "Mario", "Rossi", "0123456789", null,
                false, null, null, null, null, false);

        ContractCustomerResponse contractCustomerResponse = new ContractCustomerResponse(contractId, now.toString(),
                ContractStatus.CONFIRMED, null, customerResponse, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        when(contractService.getContractCustomerResponseById(contractId)).thenReturn(contractCustomerResponse);

        Response response = contractController.getContractById(contractId);

        assertEquals(200, response.getStatus());
        assertEquals(contractCustomerResponse, response.getEntity());
    }

    @Test
    void getContractById_AsCustomer_Returns403WhenNotOwned() {
        UUID anotherCustomerId = UUID.randomUUID();
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());

        CustomerResponse customerResponse = new CustomerResponse(anotherCustomerId, "Luigi", "Verdi", "1234567890",
                null, false, null, null, null, null, false);

        ContractCustomerResponse contractCustomerResponse = new ContractCustomerResponse(contractId, now.toString(),
                ContractStatus.CONFIRMED, null, customerResponse, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        when(contractService.getContractCustomerResponseById(contractId)).thenReturn(contractCustomerResponse);

        Response response = contractController.getContractById(contractId);

        assertEquals(403, response.getStatus());
        verify(contractService).getContractCustomerResponseById(contractId);
    }

    @Test
    void getContractById_AsCustomer_Returns403WhenStatusDraft() {
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());

        CustomerResponse customerResponse = new CustomerResponse(customerId, "Mario", "Rossi", "0123456789", null,
                false, null, null, null, null, false);

        ContractCustomerResponse contractCustomerResponse = new ContractCustomerResponse(contractId, now.toString(),
                ContractStatus.DRAFT, null, customerResponse, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        when(contractService.getContractCustomerResponseById(contractId)).thenReturn(contractCustomerResponse);

        Response response = contractController.getContractById(contractId);

        assertEquals(403, response.getStatus());
    }

    @Test
    void addContract_Returns201AndLocationHeader() {
        SalesDocumentCreateRequest request = new SalesDocumentCreateRequest(vehicleId, customerId);
        when(contractService.addContract(request)).thenReturn(contractResponse);

        Response response = contractController.addContract(request);

        assertEquals(201, response.getStatus());
        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/contracts/" + contractId));
    }

    @Test
    void createContractFromQuotation_Returns201AndLocationHeader() {
        when(contractService.createContractFromQuotation(quotationId)).thenReturn(contractResponse);

        Response response = contractController.createContractFromQuotation(quotationId);

        assertEquals(201, response.getStatus());
        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/contracts/" + contractId));
    }

    @Test
    void updateContract_Returns200() {
        ContractUpdateRequest request = new ContractUpdateRequest(now, now, vehicleId, customerId, BigDecimal.ZERO,
                "Pub", "Int", BigDecimal.TEN, null, null);
        when(contractService.updateContract(contractId, request)).thenReturn(contractResponse);

        Response response = contractController.updateContract(contractId, request);

        assertEquals(200, response.getStatus());
        verify(contractService).updateContract(contractId, request);
    }

    @Test
    void confirmContract_Returns200() {
        ContractConfirmRequest request = new ContractConfirmRequest(BigDecimal.TEN, now);
        when(contractService.confirmContract(contractId, BigDecimal.TEN, now)).thenReturn(contractResponse);

        Response response = contractController.confirmContract(contractId, request);

        assertEquals(200, response.getStatus());
        verify(contractService).confirmContract(contractId, BigDecimal.TEN, now);
    }

    @Test
    void completeContract_Returns200() {
        when(contractService.completeContract(contractId)).thenReturn(contractResponse);
        Response response = contractController.completeContract(contractId);
        assertEquals(200, response.getStatus());
    }

    @Test
    void cancelContract_Returns200() {
        ContractCancelRequest request = new ContractCancelRequest("Ritirato dal cliente");
        when(contractService.cancelContract(contractId, "Ritirato dal cliente")).thenReturn(contractResponse);
        Response response = contractController.cancelContract(contractId, request);
        assertEquals(200, response.getStatus());
    }

    @Test
    void archiveContract_Returns200() {
        when(contractService.archiveContract(contractId)).thenReturn(contractResponse);
        Response response = contractController.archiveContract(contractId);
        assertEquals(200, response.getStatus());
    }

    @Test
    void unarchiveContract_Returns200() {
        when(contractService.unarchiveContract(contractId)).thenReturn(contractResponse);
        Response response = contractController.unarchiveContract(contractId);
        assertEquals(200, response.getStatus());
    }

    // payments

    @Test
    void addPaymentToContract_Returns201AndLocationHeader() {
        PaymentRecordRequest request = new PaymentRecordRequest("Acconto", BigDecimal.TEN, now);
        when(contractService.addPaymentToContract(contractId, "Acconto", BigDecimal.TEN, now))
                .thenReturn(transactionResponse);

        Response response = contractController.addPaymentToContract(contractId, request);

        assertEquals(201, response.getStatus());
        assertTrue(response.getLocation().toString().endsWith("/transactions/" + transactionId));
    }

    @Test
    void addRefundToContract_Returns201AndLocationHeader() {
        PaymentRecordRequest request = new PaymentRecordRequest("Rimborso", BigDecimal.TEN, now);
        when(contractService.addRefundToContract(contractId, "Rimborso", BigDecimal.TEN, now))
                .thenReturn(transactionResponse);

        Response response = contractController.addRefundToContract(contractId, request);

        assertEquals(201, response.getStatus());
        assertTrue(response.getLocation().toString().endsWith("/transactions/" + transactionId));
    }

    // items

    @Test
    void addItemsToContract_Returns200() {
        CatalogItemIdsRequest request = new CatalogItemIdsRequest(Set.of(itemId));
        when(contractService.addItemsToContract(contractId, Set.of(itemId))).thenReturn(contractResponse);

        Response response = contractController.addItemsToContract(contractId, request);

        assertEquals(200, response.getStatus());
        verify(contractService).addItemsToContract(contractId, Set.of(itemId));
    }

    @Test
    void updateAppliedItemPrice_Returns200() {
        CatalogItemPriceUpdateRequest request = new CatalogItemPriceUpdateRequest(BigDecimal.TEN);
        when(contractService.updateAppliedItemPrice(contractId, itemId, BigDecimal.TEN)).thenReturn(contractResponse);

        Response response = contractController.updateAppliedItemPrice(contractId, itemId, request);

        assertEquals(200, response.getStatus());
        verify(contractService).updateAppliedItemPrice(contractId, itemId, BigDecimal.TEN);
    }

    @Test
    void removeItemsFromContract_Returns200() {
        CatalogItemIdsRequest request = new CatalogItemIdsRequest(Set.of(itemId));
        when(contractService.removeItemsFromContract(contractId, Set.of(itemId))).thenReturn(contractResponse);

        Response response = contractController.removeItemsFromContract(contractId, request);

        assertEquals(200, response.getStatus());
        verify(contractService).removeItemsFromContract(contractId, Set.of(itemId));
    }

    // helper methods

    private ContractResponse buildContractResponse(UUID id) {
        return new ContractResponse(id, null, null, null, null, null, null, false, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private ContractCustomerResponse buildContractCustomerResponse(UUID id) {
        return new ContractCustomerResponse(id, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private TransactionResponse buildTransactionResponse(UUID id) {
        return new TransactionResponse(id, "Pagamento", BigDecimal.TEN, now.toString(),
                TransactionType.IN, null, contractId);
    }
}