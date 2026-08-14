package com.autosalone.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.requests.AccessoryPackageRequest;
import com.autosalone.dtos.requests.AccessoryRequest;
import com.autosalone.enums.CatalogItemType;
import com.autosalone.models.catalog.Accessory;
import com.autosalone.models.catalog.AccessoryPackage;
import com.autosalone.models.catalog.PurchasableItem;
import com.autosalone.services.CatalogService;

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
    public Response getCatalogItems(
            @QueryParam("keyword") @Size(max = 50, message = "The keyword must not be over 50 characters") String keyword,
            @QueryParam("isArchived") Boolean isArchived,
            @QueryParam("itemType") CatalogItemType itemType) {
        List<PurchasableItem> items = catalogService.getPurchasableItems(keyword, isArchived, itemType);
        return Response.ok(items).build(); // 200 OK
    }

    @GET
    @Path("/{id}")
    public Response getCatalogItemById(@PathParam("id") UUID id) {
        PurchasableItem item = catalogService.getItemById(id);
        return Response.ok(item).build(); // 200 OK
    }

    // accessory

    @POST
    @Path("/accessories")
    public Response addAccessory(@Valid AccessoryRequest request) {
        Accessory accessory = catalogService.addAccessory(request);

        URI location = URI.create("/catalog/" + accessory.getId());
        return Response.created(location).entity(accessory).build(); // 201 Created
    }

    @PUT
    @Path("/accessories/{id}")
    public Response updateAccessory(@PathParam("id") UUID id, @Valid AccessoryRequest request) {
        Accessory accessory = catalogService.updateAccessory(id, request);
        return Response.ok(accessory).build(); // 200 OK
    }

    // accessory package

    @POST
    @Path("/accessory-packages")
    public Response addAccessoryPackage(@Valid AccessoryPackageRequest request) {
        AccessoryPackage accessoryPackage = catalogService.addAccessoryPackage(request);

        URI location = URI.create("/catalog/" + accessoryPackage.getId());
        return Response.created(location).entity(accessoryPackage).build(); // 201 Created
    }

    @PUT
    @Path("/accessory-packages/{id}")
    public Response updateAccessoryPackage(@PathParam("id") UUID id, @Valid AccessoryPackageRequest request) {
        AccessoryPackage accessoryPackage = catalogService.updateAccessoryPackage(id, request);
        return Response.ok(accessoryPackage).build(); // 200 OK
    }

    @DELETE
    @Path("/{id}")
    public Response removeCatalogItem(@PathParam("id") UUID id) {
        catalogService.removePurchasableItem(id);
        return Response.noContent().build(); // 204 No Content
    }

}