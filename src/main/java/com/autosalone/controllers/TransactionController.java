package com.autosalone.controllers;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.autosalone.dtos.requests.GeneralTransactionRequest;
import com.autosalone.dtos.responses.TransactionResponse;
import com.autosalone.enums.SortOrder;
import com.autosalone.enums.TransactionType;
import com.autosalone.services.TransactionService;
import com.autosalone.utils.Utils;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionController {

    @Inject
    private TransactionService transactionService;

    @GET
    @RolesAllowed("OWNER")
    public Response getTransactions(
            @QueryParam("dateFrom") String dateFromString,
            @QueryParam("dateTo") String dateToString,
            @QueryParam("type") TransactionType type,
            @QueryParam("sortOrder") @DefaultValue("DESC") SortOrder sortOrder) {

        LocalDate dateFrom = Utils.parseDate(dateFromString);
        LocalDate dateTo = Utils.parseDate(dateToString);
        List<TransactionResponse> transactions = transactionService.getTransactions(dateFrom, dateTo, type, sortOrder);
        return Response.ok(transactions).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("OWNER")
    public Response getTransactionById(@PathParam("id") UUID id) {
        TransactionResponse transaction = transactionService.getTransactionResponseById(id);
        return Response.ok(transaction).build(); // 200 OK
    }

    @GET
    @Path("/summary")
    @RolesAllowed("OWNER")
    public Response getSummary(
            @QueryParam("dateFrom") String dateFromString,
            @QueryParam("dateTo") String dateToString) {

        LocalDate dateFrom = Utils.parseDate(dateFromString);
        LocalDate dateTo = Utils.parseDate(dateToString);

        BigDecimal incomes = transactionService.getSumOfIncomes(dateFrom, dateTo);
        BigDecimal expenses = transactionService.getSumOfExpenses(dateFrom, dateTo);

        return Response.ok(Map.of(
                "totalIncomes", incomes != null ? incomes : BigDecimal.ZERO,
                "totalExpenses", expenses != null ? expenses : BigDecimal.ZERO)).build(); // 200 OK
    }

    @POST
    @Path("/expenses")
    @RolesAllowed("OWNER")
    public Response createGeneralExpense(@Valid GeneralTransactionRequest request) {
        TransactionResponse transaction = transactionService.createGeneralExpense(
                request.reason(), request.amount(), request.date());
        URI location = URI.create("/transactions/" + transaction.id());
        return Response.created(location).entity(transaction).build(); // 201 Created
    }

    @POST
    @Path("/incomes")
    @RolesAllowed("OWNER")
    public Response createGeneralIncome(@Valid GeneralTransactionRequest request) {
        TransactionResponse transaction = transactionService.createGeneralIncome(
                request.reason(), request.amount(), request.date());
        URI location = URI.create("/transactions/" + transaction.id());
        return Response.created(location).entity(transaction).build(); // 201 Created
    }
}