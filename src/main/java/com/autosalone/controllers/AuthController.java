package com.autosalone.controllers;

import com.autosalone.dtos.ForgotPasswordRequest;
import com.autosalone.dtos.LoginRequest;
import com.autosalone.dtos.ResetPasswordRequest;
import com.autosalone.dtos.SignUpRequest;
import com.autosalone.enums.TokenType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.AuthToken;
import com.autosalone.models.User;
import com.autosalone.services.AuthTokenService;
import com.autosalone.services.EmailService;
import com.autosalone.services.UserService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    private UserService userService;

    @Inject
    private AuthTokenService authTokenService;

    @Inject
    private EmailService emailService;

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        User user = userService.login(request.email(), request.password());

        // TODO generare un vero JWT token
        String jwtToken = "mock-jwt-token-" + user.getId();

        return Response.ok(jwtToken).build(); // 200 OK
    }

    @POST
    @Path("/signup")
    public Response signUp(@Valid SignUpRequest request) {
        userService.completeRegistration(request.token(), request.newPassword());
        return Response.noContent().build(); // 204 No Content
    }

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(@Valid ForgotPasswordRequest request) {
        try {
            User user = userService.getUserByEmail(request.email());
            AuthToken token = authTokenService.createPasswordResetToken(user);

            emailService.sendPasswordReset(user.getEmail(), token.getToken());

        } catch (ResourceNotFoundException e) {
            // ignorato per sicurezza
        }

        return Response.noContent().build(); // 204 No Content
    }

    @POST
    @Path("/reset-password")
    public Response resetPassword(@Valid ResetPasswordRequest request) {
        AuthToken token = authTokenService.validateToken(request.token(), TokenType.PASSWORD_RESET);

        userService.updatePassword(token.getUser().getId(), request.newPassword());

        authTokenService.deleteToken(token);

        return Response.noContent().build(); // 204 No Content
    }
}