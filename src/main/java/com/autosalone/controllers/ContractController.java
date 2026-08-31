package com.autosalone.controllers;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.requests.ContractCancelRequest;
import com.autosalone.dtos.requests.ContractConfirmRequest;
import com.autosalone.dtos.requests.ContractUpdateRequest;
import com.autosalone.dtos.requests.CatalogItemIdsRequest;
import com.autosalone.dtos.requests.CatalogItemPriceUpdateRequest;
import com.autosalone.dtos.requests.PaymentRecordRequest;
import com.autosalone.dtos.requests.SalesDocumentCreateRequest;
import com.autosalone.dtos.responses.ContractCustomerResponse;
import com.autosalone.dtos.responses.ContractResponse;
import com.autosalone.dtos.responses.TransactionResponse;
import com.autosalone.enums.ContractStatus;
import com.autosalone.services.ContractService;
import com.autosalone.utils.Utils;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@ApplicationScoped
@Path("/contracts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContractController {

    @Inject
    private ContractService contractService;

    @Context
    private SecurityContext securityContext;

    @GET
    @RolesAllowed({ "OWNER", "CUSTOMER" })
    public Response getContracts(
            @QueryParam("dateFrom") String dateFromString,
            @QueryParam("dateTo") String dateToString,
            @QueryParam("isArchived") Boolean isArchived,
            @QueryParam("vehicleId") UUID vehicleId,
            @QueryParam("customerId") UUID customerId,
            @QueryParam("statusList") List<ContractStatus> statusList) {

        LocalDate dateFrom = Utils.parseDate(dateFromString);
        LocalDate dateTo = Utils.parseDate(dateToString);

        if (securityContext.isUserInRole("CUSTOMER")) {
            customerId = UUID.fromString(securityContext.getUserPrincipal().getName());

            List<ContractStatus> filteredStatusList = (statusList == null || statusList.isEmpty())
                    ? List.of(ContractStatus.CONFIRMED, ContractStatus.COMPLETED, ContractStatus.CANCELED,
                            ContractStatus.VOIDED)
                    : statusList.stream().filter(status -> status != ContractStatus.DRAFT).toList();

            if (filteredStatusList.isEmpty()) {
                return Response.ok(List.of()).build(); // 200 OK
            }

            List<ContractCustomerResponse> contracts = contractService.getContractsForCustomer(
                    dateFrom, dateTo, isArchived, vehicleId, customerId, filteredStatusList);
            return Response.ok(contracts).build(); // 200 OK
        }

        List<ContractResponse> contracts = contractService.getContractsForOwner(dateFrom, dateTo, isArchived, vehicleId,
                customerId, statusList);
        return Response.ok(contracts).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "OWNER", "CUSTOMER" })
    public Response getContractById(@PathParam("id") UUID id) {

        if (securityContext.isUserInRole("CUSTOMER")) {
            String loggedInUserId = securityContext.getUserPrincipal().getName();

            ContractCustomerResponse contract = contractService.getContractCustomerResponseById(id);

            if (!contract.customer().id().toString().equals(loggedInUserId)
                    || contract.status() == ContractStatus.DRAFT) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"You do not have access to this contract\"}").build();
            }

            return Response.ok(contract).build();// 200 OK
        }

        ContractResponse contract = contractService.getContractResponseById(id);
        return Response.ok(contract).build(); // 200 OK
    }

    @POST
    @RolesAllowed("OWNER")
    public Response addContract(@Valid SalesDocumentCreateRequest request) {
        ContractResponse contract = contractService.addContract(request);
        URI location = URI.create("/contracts/" + contract.id());
        return Response.created(location).entity(contract).build(); // 201 Created
    }

    @POST
    @Path("/from-quotation/{quotationId}")
    @RolesAllowed("OWNER")
    public Response createContractFromQuotation(@PathParam("quotationId") UUID quotationId) {
        ContractResponse contract = contractService.createContractFromQuotation(quotationId);
        URI location = URI.create("/contracts/" + contract.id());
        return Response.created(location).entity(contract).build(); // 201 Created
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("OWNER")
    public Response updateContract(@PathParam("id") UUID id, @Valid ContractUpdateRequest request) {
        ContractResponse contract = contractService.updateContract(id, request);
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/confirm")
    @RolesAllowed("OWNER")
    public Response confirmContract(@PathParam("id") UUID id, @Valid ContractConfirmRequest request) {
        ContractResponse contract = contractService.confirmContract(id, request.depositAmount(), request.depositDate());
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/complete")
    @RolesAllowed("OWNER")
    public Response completeContract(@PathParam("id") UUID id) {
        ContractResponse contract = contractService.completeContract(id);
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/cancel")
    @RolesAllowed("OWNER")
    public Response cancelContract(@PathParam("id") UUID id, @Valid ContractCancelRequest request) {
        ContractResponse contract = contractService.cancelContract(id, request.reason());
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/archive")
    @RolesAllowed("OWNER")
    public Response archiveContract(@PathParam("id") UUID id) {
        ContractResponse contract = contractService.archiveContract(id);
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/unarchive")
    @RolesAllowed("OWNER")
    public Response unarchiveContract(@PathParam("id") UUID id) {
        ContractResponse contract = contractService.unarchiveContract(id);
        return Response.ok(contract).build(); // 200 OK
    }

    // payments

    @POST
    @Path("/{id}/payments")
    @RolesAllowed("OWNER")
    public Response addPaymentToContract(@PathParam("id") UUID id, @Valid PaymentRecordRequest request) {
        TransactionResponse payment = contractService.addPaymentToContract(id, request.description(), request.amount(),
                request.date());
        URI location = URI.create("/transactions/" + payment.id());
        return Response.created(location).entity(payment).build(); // 201 Created
    }

    @POST
    @Path("/{id}/refunds")
    @RolesAllowed("OWNER")
    public Response addRefundToContract(@PathParam("id") UUID id, @Valid PaymentRecordRequest request) {
        TransactionResponse refund = contractService.addRefundToContract(id, request.description(), request.amount(),
                request.date());
        URI location = URI.create("/transactions/" + refund.id());
        return Response.created(location).entity(refund).build(); // 201 Created
    }

    // items

    @POST
    @Path("/{id}/items")
    @RolesAllowed("OWNER")
    public Response addItemsToContract(@PathParam("id") UUID id, @Valid CatalogItemIdsRequest request) {
        ContractResponse contract = contractService.addItemsToContract(id, request.catalogItemIds());
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/items/{itemId}/price")
    @RolesAllowed("OWNER")
    public Response updateAppliedItemPrice(
            @PathParam("id") UUID contractId,
            @PathParam("itemId") UUID itemId,
            @Valid CatalogItemPriceUpdateRequest request) {
        ContractResponse contract = contractService.updateAppliedItemPrice(contractId, itemId, request.newPrice());
        return Response.ok(contract).build(); // 200 OK
    }

    @POST
    @Path("/{id}/items/remove")
    @RolesAllowed("OWNER")
    public Response removeItemsFromContract(@PathParam("id") UUID id, @Valid CatalogItemIdsRequest request) {
        ContractResponse contract = contractService.removeItemsFromContract(id, request.catalogItemIds());
        return Response.ok(contract).build(); // 200 OK
    }
}