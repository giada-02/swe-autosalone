package com.autosalone.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.requests.AccessoryPackageRequest;
import com.autosalone.dtos.requests.AccessoryRequest;
import com.autosalone.dtos.responses.CatalogItemResponse;
import com.autosalone.enums.CatalogItemType;
import com.autosalone.services.CatalogService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/catalog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogController {

    @Inject
    private CatalogService catalogService;

    @GET
    @RolesAllowed("OWNER")
    public Response getCatalogItems(
            @QueryParam("keyword") @Size(max = 50, message = "The keyword must not be over 50 characters") String keyword,
            @QueryParam("isArchived") Boolean isArchived,
            @QueryParam("itemType") CatalogItemType itemType) {
        List<CatalogItemResponse> items = catalogService.getPurchasableItems(keyword, isArchived, itemType);
        return Response.ok(items).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("OWNER")
    public Response getCatalogItemById(@PathParam("id") UUID id) {
        CatalogItemResponse item = catalogService.getItemResponseById(id);
        return Response.ok(item).build(); // 200 OK
    }

    // accessory

    @POST
    @Path("/accessories")
    @RolesAllowed("OWNER")
    public Response addAccessory(@Valid AccessoryRequest request) {
        CatalogItemResponse accessory = catalogService.addAccessory(request);

        URI location = URI.create("/catalog/" + accessory.id());
        return Response.created(location).entity(accessory).build(); // 201 Created
    }

    @PUT
    @Path("/accessories/{id}")
    @RolesAllowed("OWNER")
    public Response updateAccessory(@PathParam("id") UUID id, @Valid AccessoryRequest request) {
        CatalogItemResponse accessory = catalogService.updateAccessory(id, request);
        return Response.ok(accessory).build(); // 200 OK
    }

    // accessory package

    @POST
    @Path("/accessory-packages")
    @RolesAllowed("OWNER")
    public Response addAccessoryPackage(@Valid AccessoryPackageRequest request) {
        CatalogItemResponse accessoryPackage = catalogService.addAccessoryPackage(request);

        URI location = URI.create("/catalog/" + accessoryPackage.id());
        return Response.created(location).entity(accessoryPackage).build(); // 201 Created
    }

    @PUT
    @Path("/accessory-packages/{id}")
    @RolesAllowed("OWNER")
    public Response updateAccessoryPackage(@PathParam("id") UUID id, @Valid AccessoryPackageRequest request) {
        CatalogItemResponse accessoryPackage = catalogService.updateAccessoryPackage(id, request);
        return Response.ok(accessoryPackage).build(); // 200 OK
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("OWNER")
    public Response removeCatalogItem(@PathParam("id") UUID id) {
        catalogService.removePurchasableItem(id);
        return Response.noContent().build(); // 204 No Content
    }

}