package com.autosalone.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.requests.CustomerRequest;
import com.autosalone.dtos.responses.CustomerListResponse;
import com.autosalone.dtos.responses.CustomerResponse;
import com.autosalone.services.CustomerService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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
            @QueryParam("keyword") @Size(max = 50, message = "The keyword must not be over 50 characters") String keyword,
            @QueryParam("isActive") Boolean isActive) {
        List<CustomerListResponse> customers = customerService.getCustomers(keyword, isActive);
        return Response.ok(customers).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    public Response getCustomerById(@PathParam("id") UUID id) {
        CustomerResponse customer = customerService.getCustomerResponseById(id);
        return Response.ok(customer).build(); // 200 OK
    }

    @POST
    public Response addCustomer(@Valid CustomerRequest request) {
        CustomerResponse customer = customerService.addCustomer(request);

        URI location = URI.create("/customers/" + customer.id());
        return Response.created(location).entity(customer).build(); // 201 Created
    }

    @PUT
    @Path("/{id}")
    public Response updateCustomer(@PathParam("id") UUID id, @Valid CustomerRequest request) {
        CustomerResponse customer = customerService.updateCustomer(id, request);
        return Response.ok(customer).build(); // 200 OK
    }
}