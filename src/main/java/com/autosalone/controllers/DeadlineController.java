package com.autosalone.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.DeadlineCompletionRequest;
import com.autosalone.dtos.DeadlineRequest;
import com.autosalone.models.Deadline;
import com.autosalone.services.DeadlineService;

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
    public Response getUrgentDeadlines(@QueryParam("upToDate") LocalDate upToDate) {
        if (upToDate == null) {
            upToDate = LocalDate.now().plusDays(30); // default: scadenze dei prossimi 30 giorni
        }
        List<Deadline> deadlines = deadlineService.getUrgentDeadlines(upToDate);
        return Response.ok(deadlines).build(); // 200 OK
    }

    @PUT
    @Path("/{id}")
    public Response updateDeadline(@PathParam("id") UUID id, @Valid DeadlineRequest request) {
        Deadline deadline = deadlineService.updateDeadline(id, request);
        return Response.ok(deadline).build(); // 200 OK
    }

    @POST
    @Path("/{id}/complete")
    public Response completeDeadline(
            @PathParam("id") UUID id, DeadlineCompletionRequest request) {
        deadlineService.completeDeadline(id, request.completionDate(), request.notes());
        return Response.ok().build(); // 200 OK
    }
}