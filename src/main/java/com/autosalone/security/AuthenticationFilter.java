package com.autosalone.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.autosalone.dtos.errors.ApiErrorResponse;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.User;
import com.autosalone.services.JwtService;
import com.autosalone.services.UserService;
import com.autosalone.utils.AuditContext;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.security.Principal;
import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class AuthenticationFilter implements ContainerRequestFilter {

    @Inject
    private JwtService jwtService;

    @Inject
    private UserService userService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authorizationHeader.substring("Bearer".length()).trim();

        try {
            DecodedJWT decodedJWT = jwtService.validateToken(token);

            String userId = decodedJWT.getSubject();
            UUID userUuid = UUID.fromString(userId);
            String role = decodedJWT.getClaim("role").asString();

            User user = userService.getUserById(userUuid);
            if (!user.isActive() || !user.acceptsTokenIssuedAt(decodedJWT.getIssuedAtAsInstant())) {
                abortUnauthorized(requestContext, "Session is no longer valid, please log in again");
                return;
            }

            AuditContext.setCurrentUserId(userUuid);

            SecurityContext originalContext = requestContext.getSecurityContext();

            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> userId;
                }

                @Override
                public boolean isUserInRole(String roleName) {
                    return role.equals(roleName);
                }

                @Override
                public boolean isSecure() {
                    return originalContext.isSecure();
                }

                @Override
                public String getAuthenticationScheme() {
                    return "Bearer";
                }
            });

        } catch (JWTVerificationException | ResourceNotFoundException | IllegalArgumentException e) {
            abortUnauthorized(requestContext, null);
        }
    }

    private void abortUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ApiErrorResponse(401, message == null ? "Invalid or expired token" : message))
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }
}