package com.autosalone.controllers;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.requests.CatalogItemIdsRequest;
import com.autosalone.dtos.requests.CatalogItemPriceUpdateRequest;
import com.autosalone.dtos.requests.QuotationUpdateRequest;
import com.autosalone.dtos.requests.SalesDocumentCreateRequest;
import com.autosalone.dtos.responses.QuotationResponse;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.services.QuotationService;
import com.autosalone.utils.Utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/quotations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuotationController {

    @Inject
    private QuotationService quotationService;

    @GET
    public Response getQuotations(
            @QueryParam("dateFrom") String dateFromString,
            @QueryParam("dateTo") String dateToString,
            @QueryParam("isArchived") Boolean isArchived,
            @QueryParam("vehicleId") UUID vehicleId,
            @QueryParam("customerId") UUID customerId,
            @QueryParam("statusList") List<QuotationStatus> statusList) {

        LocalDate dateFrom = Utils.parseDate(dateFromString);
        LocalDate dateTo = Utils.parseDate(dateToString);
        List<QuotationResponse> quotations = quotationService.getQuotations(dateFrom, dateTo, isArchived, vehicleId,
                customerId, statusList);
        return Response.ok(quotations).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    public Response getQuotationById(@PathParam("id") UUID id) {
        QuotationResponse quotation = quotationService.getQuotationResponseById(id);
        return Response.ok(quotation).build(); // 200 OK
    }

    @POST
    public Response addQuotation(@Valid SalesDocumentCreateRequest request) {
        QuotationResponse quotation = quotationService.addQuotation(request);
        URI location = URI.create("/quotations/" + quotation.id());
        return Response.created(location).entity(quotation).build(); // 201 Created
    }

    @PUT
    @Path("/{id}")
    public Response updateQuotation(@PathParam("id") UUID id, @Valid QuotationUpdateRequest request) {
        QuotationResponse quotation = quotationService.updateQuotation(id, request);
        return Response.ok(quotation).build(); // 200 OK
    }

    @POST
    @Path("/{id}/clone")
    public Response cloneQuotation(@PathParam("id") UUID id) {
        QuotationResponse quotation = quotationService.cloneQuotation(id);
        URI location = URI.create("/quotations/" + quotation.id());
        return Response.created(location).entity(quotation).build(); // 201 Created
    }

    @PUT
    @Path("/{id}/issue")
    public Response issueQuotation(@PathParam("id") UUID id) {
        QuotationResponse quotation = quotationService.issueQuotation(id);
        return Response.ok(quotation).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/archive")
    public Response archiveQuotation(@PathParam("id") UUID id) {
        QuotationResponse quotation = quotationService.archiveQuotation(id);
        return Response.ok(quotation).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/unarchive")
    public Response unarchiveQuotation(@PathParam("id") UUID id) {
        QuotationResponse quotation = quotationService.unarchiveQuotation(id);
        return Response.ok(quotation).build(); // 200 OK
    }

    // items

    @POST
    @Path("/{id}/items")
    public Response addItemsToQuotation(@PathParam("id") UUID id, @Valid CatalogItemIdsRequest request) {
        QuotationResponse quotation = quotationService.addItemsToQuotation(id, request.catalogItemIds());
        return Response.ok(quotation).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/items/{itemId}/price")
    public Response updateAppliedItemPrice(
            @PathParam("id") UUID quotationId,
            @PathParam("itemId") UUID itemId,
            @Valid CatalogItemPriceUpdateRequest request) {
        QuotationResponse quotation = quotationService.updateAppliedItemPrice(quotationId, itemId, request.newPrice());
        return Response.ok(quotation).build(); // 200 OK
    }

    @POST
    @Path("/{id}/items/remove")
    public Response removeItemsFromQuotation(@PathParam("id") UUID id, @Valid CatalogItemIdsRequest request) {
        QuotationResponse quotation = quotationService.removeItemsFromQuotation(id, request.catalogItemIds());
        return Response.ok(quotation).build(); // 200 OK
    }
}