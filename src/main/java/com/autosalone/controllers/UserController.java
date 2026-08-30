package com.autosalone.controllers;

import java.util.UUID;

import com.autosalone.dtos.requests.PasswordUpdateRequest;
import com.autosalone.models.AuthToken;
import com.autosalone.models.User;
import com.autosalone.services.AuthTokenService;
import com.autosalone.services.EmailService;
import com.autosalone.services.UserService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    private UserService userService;

    @Inject
    private AuthTokenService authTokenService;

    @Inject
    private EmailService emailService;

    @PUT
    @Path("/{id}/password")
    public Response updatePassword(@PathParam("id") UUID id, @Valid PasswordUpdateRequest request) {
        userService.updatePassword(id, request.currentPassword(), request.newPassword());
        return Response.noContent().build(); // 204 No Content
    }

    @PUT
    @Path("/{id}/deactivate")
    public Response deactivateUser(@PathParam("id") UUID id) {
        userService.deactivateUser(id);
        return Response.noContent().build(); // 204 No Content
    }

    @POST
    @Path("/{id}/invite")
    public Response sendRegistrationInvite(@PathParam("id") UUID id) {
        User user = userService.getUserById(id);

        AuthToken token = authTokenService.createRegistrationToken(user);

        emailService.sendRegistrationInvite(user.getEmail(), token.getToken());

        return Response.noContent().build(); // 204 No Content
    }

}