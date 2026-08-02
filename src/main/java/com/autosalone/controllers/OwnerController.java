package com.autosalone.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.OwnerListResponse;
import com.autosalone.dtos.OwnerRequest;
import com.autosalone.dtos.OwnerResponse;
import com.autosalone.services.OwnerService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/owners")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OwnerController {

    @Inject
    private OwnerService ownerService;

    @GET
    public Response getOwners(@QueryParam("isActive") Boolean isActive) {
        List<OwnerListResponse> owners = ownerService.getOwners(isActive);
        return Response.ok(owners).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    public Response getOwnerById(@PathParam("id") UUID id) {
        OwnerResponse owner = ownerService.getOwnerResponseById(id);
        return Response.ok(owner).build(); // 200 OK
    }

    @POST
    public Response addOwner(@Valid OwnerRequest request) {
        OwnerResponse owner = ownerService.addOwner(request);

        URI location = URI.create("/owners/" + owner.id());
        return Response.created(location).entity(owner).build(); // 201 Created
    }

    @PUT
    @Path("/{id}")
    public Response updateOwner(@PathParam("id") UUID id, @Valid OwnerRequest request) {
        OwnerResponse owner = ownerService.updateOwner(id, request);
        return Response.ok(owner).build(); // 200 OK
    }
}