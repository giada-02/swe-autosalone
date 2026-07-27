package com.autosalone.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.CustomerRequest;
import com.autosalone.models.Customer;
import com.autosalone.services.CustomerService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerController {

    @Inject
    private CustomerService customerService;

    @GET
    public Response getCustomers(
            @QueryParam("keyword") String keyword,
            @QueryParam("isActive") Boolean isActive) {
        List<Customer> customers = customerService.getCustomers(keyword, isActive);
        return Response.ok(customers).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    public Response getCustomerById(@PathParam("id") UUID id) {
        Customer customer = customerService.getCustomerById(id);
        return Response.ok(customer).build(); // 200 OK
    }

    @POST
    public Response addCustomer(@Valid CustomerRequest request) {
        UUID newCustomerId = customerService.addCustomer(request);

        URI location = URI.create("/customers/" + newCustomerId);
        return Response.created(location)
                .entity(java.util.Map.of("id", newCustomerId))
                .build(); // 201 OK (Created)
    }

    @PUT
    @Path("/{id}")
    public Response updateCustomer(@PathParam("id") UUID id, @Valid CustomerRequest request) {
        customerService.updateCustomer(id, request);
        return Response.noContent().build(); // 204 OK (No Content)
    }
}