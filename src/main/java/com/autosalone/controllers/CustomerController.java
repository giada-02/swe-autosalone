package com.autosalone.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.requests.CustomerRequest;
import com.autosalone.dtos.responses.CustomerListResponse;
import com.autosalone.dtos.responses.CustomerResponse;
import com.autosalone.services.CustomerService;

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
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerController {

    @Inject
    private CustomerService customerService;

    @Context
    private SecurityContext securityContext;

    @GET
    @RolesAllowed("OWNER")
    public Response getCustomers(
            @QueryParam("keyword") @Size(max = 50, message = "The keyword must not be over 50 characters") String keyword,
            @QueryParam("isActive") Boolean isActive) {
        List<CustomerListResponse> customers = customerService.getCustomers(keyword, isActive);
        return Response.ok(customers).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "OWNER", "CUSTOMER" })
    public Response getCustomerById(@PathParam("id") UUID id) {
        String loggedInUserId = securityContext.getUserPrincipal().getName();

        if (securityContext.isUserInRole("CUSTOMER") && !loggedInUserId.equals(id.toString())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"You can only access your own profile\"}").build();
        }

        CustomerResponse customer = customerService.getCustomerResponseById(id);
        return Response.ok(customer).build(); // 200 OK
    }

    @POST
    @RolesAllowed("OWNER")
    public Response addCustomer(@Valid CustomerRequest request) {
        CustomerResponse customer = customerService.addCustomer(request);

        URI location = URI.create("/customers/" + customer.id());
        return Response.created(location).entity(customer).build(); // 201 Created
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("OWNER")
    public Response updateCustomer(@PathParam("id") UUID id, @Valid CustomerRequest request) {
        CustomerResponse customer = customerService.updateCustomer(id, request);
        return Response.ok(customer).build(); // 200 OK
    }
}