package com.autosalone.controllers;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.requests.DeadlineRequest;
import com.autosalone.dtos.requests.ExpenseRequest;
import com.autosalone.dtos.requests.PurchaseTransactionRequest;
import com.autosalone.dtos.requests.VehicleCreateRequest;
import com.autosalone.dtos.requests.VehicleUpdateRequest;
import com.autosalone.dtos.requests.VehicleWithdrawRequest;
import com.autosalone.dtos.responses.DeadlineResponse;
import com.autosalone.dtos.responses.ExpenseResponse;
import com.autosalone.dtos.responses.VehicleCustomerResponse;
import com.autosalone.dtos.responses.VehicleResponse;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.services.DeadlineService;
import com.autosalone.services.TransactionService;
import com.autosalone.services.VehicleService;
import com.autosalone.utils.Utils;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@ApplicationScoped
@Path("/vehicles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VehicleController {

    @Inject
    private VehicleService vehicleService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private DeadlineService deadlineService;

    @Context
    private SecurityContext securityContext;

    @GET
    @RolesAllowed({ "OWNER", "CUSTOMER" })
    public Response getVehicles(
            @QueryParam("keyword") @Size(max = 50, message = "The keyword must not be over 50 characters") String keyword,
            @QueryParam("brand") @Size(max = 255, message = "The keyword must not be over 255 characters") String brand,
            @QueryParam("condition") VehicleCondition condition,
            @QueryParam("maxPrice") BigDecimal maxPrice,
            @QueryParam("isInShowroom") Boolean isInShowroom,
            @QueryParam("statusList") List<VehicleStatus> statusList) {

        if (securityContext.isUserInRole("CUSTOMER")) {
            UUID customerId = UUID.fromString(securityContext.getUserPrincipal().getName());
            List<VehicleCustomerResponse> vehicles = vehicleService.getVehiclesForCustomer(customerId);
            return Response.ok(vehicles).build();
        }

        List<VehicleResponse> vehicles = vehicleService.getVehiclesForOwner(keyword, brand, condition, maxPrice,
                isInShowroom, statusList);
        return Response.ok(vehicles).build(); // 200 OK
    }

    @GET
    @RolesAllowed("OWNER")
    @Path("/brands")
    public Response getBrands() {
        List<String> brands = vehicleService.getAllBrands();
        return Response.ok(brands).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "OWNER", "CUSTOMER" })
    public Response getVehicleById(@PathParam("id") UUID id) {

        if (securityContext.isUserInRole("CUSTOMER")) {
            UUID customerId = UUID.fromString(securityContext.getUserPrincipal().getName());

            if (!vehicleService.isVehicleOwnedByCustomer(id, customerId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"You do not have access to this vehicle\"}").build();
            }

            VehicleCustomerResponse vehicle = vehicleService.getVehicleCustomerResponseById(id);
            return Response.ok(vehicle).build();
        }

        VehicleResponse vehicle = vehicleService.getVehicleResponseById(id);
        return Response.ok(vehicle).build(); // 200 OK
    }

    @POST
    @RolesAllowed("OWNER")
    public Response addVehicle(@Valid VehicleCreateRequest request) {
        VehicleResponse vehicle = vehicleService.addVehicle(request);
        URI location = URI.create("/vehicles/" + vehicle.id());
        return Response.created(location).entity(vehicle).build(); // 201 Created
    }

    @POST
    @Path("/{id}/purchase-transaction")
    @RolesAllowed("OWNER")
    public Response addPurchaseTransaction(
            @PathParam("id") UUID vehicleId,
            @Valid PurchaseTransactionRequest request) {
        VehicleResponse vehicle = vehicleService.addPurchaseTransaction(vehicleId, request);
        return Response.ok(vehicle).build(); // 200 OK
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("OWNER")
    public Response updateVehicle(@PathParam("id") UUID id, @Valid VehicleUpdateRequest request) {
        VehicleResponse vehicle = vehicleService.updateVehicle(id, request);
        return Response.ok(vehicle).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/withdraw")
    @RolesAllowed("OWNER")
    public Response withdrawVehicle(
            @PathParam("id") UUID id, @Valid VehicleWithdrawRequest request) {
        VehicleResponse vehicle = vehicleService.withdrawVehicle(id, request.reason());
        return Response.ok(vehicle).build(); // 200 OK
    }

    // expenses

    @GET
    @Path("/{id}/expenses")
    @RolesAllowed("OWNER")
    public Response getExpenses(
            @PathParam("id") UUID id) {
        List<ExpenseResponse> expenses = transactionService.getExpensesByVehicleId(id);
        return Response.ok(expenses).build(); // 200 OK
    }

    @POST
    @Path("/{id}/expenses")
    @RolesAllowed("OWNER")
    public Response addExpense(
            @PathParam("id") UUID id,
            @Valid ExpenseRequest request) {
        ExpenseResponse expense = vehicleService.addExpense(id, request.description(), request.amount(),
                request.date());
        URI location = URI.create("/vehicles/" + id + "/expenses/" + expense.id());
        return Response.created(location).entity(expense).build(); // 201 Created
    }

    // inspections and deadlines

    @POST
    @Path("/{id}/standard-inspections")
    @RolesAllowed("OWNER")
    public Response generateStandardInspection(
            @PathParam("id") UUID id,
            @QueryParam("lastInspection") String lastInspectionDateString) {
        LocalDate lastInspection = Utils.parseDate(lastInspectionDateString);
        DeadlineResponse deadline = vehicleService.generateStandardInspection(id, lastInspection);
        URI location = URI.create("/vehicles/" + id + "/deadlines/" + deadline.id());
        return Response.created(location).entity(deadline).build(); // 201 Created
    }

    @GET
    @Path("/{id}/deadlines")
    @RolesAllowed({ "OWNER", "CUSTOMER" })
    public Response getDeadlines(
            @PathParam("id") UUID id,
            @QueryParam("completed") @DefaultValue("false") boolean completed) {

        UUID currentUserId = UUID.fromString(securityContext.getUserPrincipal().getName());
        boolean isOwner = securityContext.isUserInRole("OWNER");

        if (!isOwner) {
            if (!vehicleService.isVehicleOwnedByCustomer(id, currentUserId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"You do not have access to the deadlines of this vehicle\"}").build();
            }
        }

        List<DeadlineResponse> deadlines = deadlineService.getDeadlinesByVehicleId(id, completed, currentUserId,
                isOwner);
        return Response.ok(deadlines).build(); // 200 OK
    }

    @POST
    @Path("/{id}/deadlines")
    @RolesAllowed("OWNER")
    public Response addDeadline(@PathParam("id") UUID id, @Valid DeadlineRequest request) {
        DeadlineResponse deadline = vehicleService.addDeadline(id, request);
        URI location = URI.create("/vehicles/" + id + "/deadlines/" + deadline.id());
        return Response.created(location).entity(deadline).build(); // 201 Created
    }
}