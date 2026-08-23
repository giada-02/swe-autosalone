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
import com.autosalone.dtos.responses.ContractResponse;
import com.autosalone.dtos.responses.TransactionResponse;
import com.autosalone.enums.ContractStatus;
import com.autosalone.services.ContractService;
import com.autosalone.utils.Utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/contracts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContractController {

    @Inject
    private ContractService contractService;

    @GET
    public Response getContracts(
            @QueryParam("dateFrom") String dateFromString,
            @QueryParam("dateTo") String dateToString,
            @QueryParam("isArchived") Boolean isArchived,
            @QueryParam("vehicleId") UUID vehicleId,
            @QueryParam("customerId") UUID customerId,
            @QueryParam("statusList") List<ContractStatus> statusList) {

        LocalDate dateFrom = Utils.parseDate(dateFromString);
        LocalDate dateTo = Utils.parseDate(dateToString);
        List<ContractResponse> contracts = contractService.getContracts(dateFrom, dateTo, isArchived, vehicleId,
                customerId, statusList);
        return Response.ok(contracts).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    public Response getContractById(@PathParam("id") UUID id) {
        ContractResponse contract = contractService.getContractResponseById(id);
        return Response.ok(contract).build(); // 200 OK
    }

    @POST
    public Response addContract(@Valid SalesDocumentCreateRequest request) {
        ContractResponse contract = contractService.addContract(request);
        URI location = URI.create("/contracts/" + contract.id());
        return Response.created(location).entity(contract).build(); // 201 Created
    }

    @POST
    @Path("/from-quotation/{quotationId}")
    public Response createContractFromQuotation(@PathParam("quotationId") UUID quotationId) {
        ContractResponse contract = contractService.createContractFromQuotation(quotationId);
        URI location = URI.create("/contracts/" + contract.id());
        return Response.created(location).entity(contract).build(); // 201 Created
    }

    @PUT
    @Path("/{id}")
    public Response updateContract(@PathParam("id") UUID id, @Valid ContractUpdateRequest request) {
        ContractResponse contract = contractService.updateContract(id, request);
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/confirm")
    public Response confirmContract(@PathParam("id") UUID id, @Valid ContractConfirmRequest request) {
        ContractResponse contract = contractService.confirmContract(id, request.depositAmount(), request.depositDate());
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/complete")
    public Response completeContract(@PathParam("id") UUID id) {
        ContractResponse contract = contractService.completeContract(id);
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/cancel")
    public Response cancelContract(@PathParam("id") UUID id, @Valid ContractCancelRequest request) {
        ContractResponse contract = contractService.cancelContract(id, request.reason());
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/archive")
    public Response archiveContract(@PathParam("id") UUID id) {
        ContractResponse contract = contractService.archiveContract(id);
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/unarchive")
    public Response unarchiveContract(@PathParam("id") UUID id) {
        ContractResponse contract = contractService.unarchiveContract(id);
        return Response.ok(contract).build(); // 200 OK
    }

    // payments

    @POST
    @Path("/{id}/payments")
    public Response addPaymentToContract(@PathParam("id") UUID id, @Valid PaymentRecordRequest request) {
        TransactionResponse payment = contractService.addPaymentToContract(id, request.description(), request.amount(),
                request.date());
        URI location = URI.create("/transactions/" + payment.id());
        return Response.created(location).entity(payment).build(); // 201 Created
    }

    @POST
    @Path("/{id}/refunds")
    public Response addRefundToContract(@PathParam("id") UUID id, @Valid PaymentRecordRequest request) {
        TransactionResponse refund = contractService.addRefundToContract(id, request.description(), request.amount(),
                request.date());
        URI location = URI.create("/transactions/" + refund.id());
        return Response.created(location).entity(refund).build(); // 201 Created
    }

    // items

    @POST
    @Path("/{id}/items")
    public Response addItemsToContract(@PathParam("id") UUID id, @Valid CatalogItemIdsRequest request) {
        ContractResponse contract = contractService.addItemsToContract(id, request.catalogItemIds());
        return Response.ok(contract).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/items/{itemId}/price")
    public Response updateAppliedItemPrice(
            @PathParam("id") UUID contractId,
            @PathParam("itemId") UUID itemId,
            @Valid CatalogItemPriceUpdateRequest request) {
        ContractResponse contract = contractService.updateAppliedItemPrice(contractId, itemId, request.newPrice());
        return Response.ok(contract).build(); // 200 OK
    }

    @POST
    @Path("/{id}/items/remove")
    public Response removeItemsFromContract(@PathParam("id") UUID id, @Valid CatalogItemIdsRequest request) {
        ContractResponse contract = contractService.removeItemsFromContract(id, request.catalogItemIds());
        return Response.ok(contract).build(); // 200 OK
    }
}