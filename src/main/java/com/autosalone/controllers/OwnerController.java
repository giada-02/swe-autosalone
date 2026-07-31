package com.autosalone.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.OwnerRequest;
import com.autosalone.dtos.OwnerResponse;
import com.autosalone.models.Owner;
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
        List<Owner> owners = ownerService.getOwners(isActive);
        List<OwnerResponse> response = owners.stream()
                .map(OwnerResponse::fromEntity)
                .toList();
        return Response.ok(response).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    public Response getOwnerById(@PathParam("id") UUID id) {
        Owner owner = ownerService.getOwnerById(id);
        OwnerResponse response = OwnerResponse.fromEntity(owner);
        return Response.ok(response).build(); // 200 OK
    }

    @POST
    public Response addOwner(@Valid OwnerRequest request) {
        UUID newOwnerId = ownerService.addOwner(request);

        URI location = URI.create("/owners/" + newOwnerId);
        return Response.created(location)
                .entity(java.util.Map.of("id", newOwnerId))
                .build(); // 201 Created
    }

    @PUT
    @Path("/{id}")
    public Response updateOwner(@PathParam("id") UUID id, @Valid OwnerRequest request) {
        ownerService.updateOwner(id, request);
        return Response.noContent().build(); // 204 No Content
    }
}