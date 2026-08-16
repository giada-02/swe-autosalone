package com.autosalone.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.requests.DeadlineCompletionRequest;
import com.autosalone.dtos.requests.DeadlineRequest;
import com.autosalone.dtos.responses.DeadlineCompletionResponse;
import com.autosalone.dtos.responses.DeadlineResponse;
import com.autosalone.services.DeadlineService;
import com.autosalone.utils.Utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/deadlines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeadlineController {

    @Inject
    private DeadlineService deadlineService;

    @GET
    @Path("/urgent")
    public Response getUrgentDeadlines(@QueryParam("upToDate") String upToDateString) {
        LocalDate upToDate = Utils.parseDate(upToDateString);
        if (upToDate == null) {
            upToDate = LocalDate.now().plusDays(30); // default: scadenze dei prossimi 30 giorni
        }
        List<DeadlineResponse> deadlines = deadlineService.getUrgentDeadlines(upToDate);
        return Response.ok(deadlines).build(); // 200 OK
    }

    @PUT
    @Path("/{id}")
    public Response updateDeadline(@PathParam("id") UUID id, @Valid DeadlineRequest request) {
        DeadlineResponse deadline = deadlineService.updateDeadline(id, request);
        return Response.ok(deadline).build(); // 200 OK
    }

    @POST
    @Path("/{id}/complete")
    public Response completeDeadline(
            @PathParam("id") UUID id, @Valid DeadlineCompletionRequest request) {
        DeadlineCompletionResponse response = deadlineService.completeDeadline(id, request.completionDate(),
                request.notes());
        return Response.ok(response).build(); // 200 OK
    }

    @DELETE
    @Path("/{id}")
    public Response deleteDeadline(@PathParam("id") UUID id) {
        deadlineService.deleteDeadline(id);
        return Response.noContent().build(); // 204 No Content
    }
}