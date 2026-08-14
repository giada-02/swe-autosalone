package com.autosalone.controllers;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.DeadlineRequest;
import com.autosalone.dtos.VehicleRequest;
import com.autosalone.dtos.VehicleWithdrawRequest;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Deadline;
import com.autosalone.models.Transaction;
import com.autosalone.models.Vehicle;
import com.autosalone.services.DeadlineService;
import com.autosalone.services.TransactionService;
import com.autosalone.services.VehicleService;
import com.autosalone.utils.Utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

    @GET
    public Response getVehicles(
            @QueryParam("keyword") @Size(max = 50, message = "The keyword must not be over 50 characters") String keyword,
            @QueryParam("brand") @Size(max = 255, message = "The keyword must not be over 255 characters") String brand,
            @QueryParam("condition") VehicleCondition condition,
            @QueryParam("maxPrice") BigDecimal maxPrice,
            @QueryParam("isInShowroom") Boolean isInShowroom,
            @QueryParam("statusList") List<VehicleStatus> statusList) {
        List<Vehicle> vehicles = vehicleService.getVehicles(keyword, brand, condition, maxPrice, isInShowroom,
                statusList);
        return Response.ok(vehicles).build(); // 200 OK
    }

    @GET
    @Path("/brands")
    public Response getBrands() {
        List<String> brands = vehicleService.getAllBrands();
        return Response.ok(brands).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    public Response getVehicleById(@PathParam("id") UUID id) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        return Response.ok(vehicle).build(); // 200 OK
    }

    @POST
    public Response addVehicle(@Valid VehicleRequest request) {
        Vehicle vehicle = vehicleService.addVehicle(request);
        URI location = URI.create("/vehicles/" + vehicle.getId());
        return Response.created(location).entity(vehicle).build(); // 201 Created
    }

    @PUT
    @Path("/{id}")
    public Response updateVehicle(@PathParam("id") UUID id, @Valid VehicleRequest request) {
        Vehicle vehicle = vehicleService.updateVehicle(id, request);
        return Response.ok(vehicle).build(); // 200 OK
    }

    @PUT
    @Path("/{id}/withdraw")
    public Response withdrawVehicle(
            @PathParam("id") UUID id, VehicleWithdrawRequest request) {
        vehicleService.withdrawVehicle(id, request.reason());
        return Response.noContent().build(); // 204 No Content
    }

    // expenses

    @GET
    @Path("/{id}/expenses")
    public Response getExpenses(
            @PathParam("id") UUID id) {
        List<Transaction> expenses = transactionService.getExpensesByVehicleId(id);
        return Response.ok(expenses).build(); // 200 OK
    }

    @POST
    @Path("/{id}/expenses")
    public Response addExpense(
            @PathParam("id") UUID id,
            @QueryParam("description") @NotBlank String description,
            @QueryParam("amount") @NotNull BigDecimal amount,
            @QueryParam("date") @NotBlank String dateString) {
        LocalDate date = Utils.parseDate(dateString);
        Transaction expense = vehicleService.addExpense(id, description, amount, date);
        URI location = URI.create("/vehicles/" + id + "/expenses/" + expense.getId());
        return Response.created(location).entity(expense).build(); // 201 Created
    }

    // inspections and deadlines

    @POST
    @Path("/{id}/standard-inspections")
    public Response generateStandardInspection(
            @PathParam("id") UUID id,
            @QueryParam("lastInspection") String lastInspectionDateString) {
        LocalDate lastInspection = Utils.parseDate(lastInspectionDateString);
        Deadline deadline = vehicleService.generateStandardInspection(id, lastInspection);
        URI location = URI.create("/vehicles/" + id + "/deadlines/" + deadline.getId());
        return Response.created(location).entity(deadline).build(); // 201 Created
    }

    @GET
    @Path("/{id}/deadlines")
    public Response getDeadlines(
            @PathParam("id") UUID id,
            @QueryParam("completed") @DefaultValue("false") boolean completed) {
        List<Deadline> deadlines = deadlineService.getDeadlinesByVehicleId(id, completed);
        return Response.ok(deadlines).build(); // 200 OK
    }

    @POST
    @Path("/{id}/deadlines")
    public Response addDeadline(@PathParam("id") UUID id, @Valid DeadlineRequest request) {
        Deadline deadline = vehicleService.addDeadline(id, request);
        URI location = URI.create("/vehicles/" + id + "/deadlines/" + deadline.getId());
        return Response.created(location).entity(deadline).build(); // 201 Created
    }

    @DELETE
    @Path("/{id}/deadlines/{deadlineId}")
    public Response removeDeadline(@PathParam("id") UUID id, @PathParam("deadlineId") UUID deadlineId) {
        vehicleService.removeDeadline(id, deadlineId);
        return Response.noContent().build(); // 204 No Content
    }
}