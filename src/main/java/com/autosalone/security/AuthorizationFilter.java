package com.autosalone.security;

import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.lang.reflect.Method;
import java.util.Arrays;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Method method = resourceInfo.getResourceMethod();
        Class<?> resourceClass = resourceInfo.getResourceClass();

        // @PermitAll
        if (method.isAnnotationPresent(PermitAll.class) || resourceClass.isAnnotationPresent(PermitAll.class)) {
            return;
        }

        // @DenyAll
        if (method.isAnnotationPresent(DenyAll.class) || resourceClass.isAnnotationPresent(DenyAll.class)) {
            abortWithForbidden(requestContext, "Access to this resource is permanently denied");
            return;
        }

        // @RolesAllowed
        RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
        if (rolesAllowed == null) {
            rolesAllowed = resourceClass.getAnnotation(RolesAllowed.class);
        }

        if (rolesAllowed == null) {
            return;
        }

        if (requestContext.getSecurityContext().getUserPrincipal() == null) {
            abortWithUnauthorized(requestContext, "Authentication required (Missing or invalid token)");
            return;
        }

        boolean hasPermission = Arrays.stream(rolesAllowed.value())
                .anyMatch(role -> requestContext.getSecurityContext().isUserInRole(role));

        if (!hasPermission) {
            abortWithForbidden(requestContext, "Permission denied");
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"" + message + "\"}")
                        .type("application/json")
                        .build());
    }

    private void abortWithForbidden(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"" + message + "\"}")
                        .type("application/json")
                        .build());
    }
}