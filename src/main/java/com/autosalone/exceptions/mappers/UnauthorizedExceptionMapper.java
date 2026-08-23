package com.autosalone.exceptions.mappers;

import com.autosalone.dtos.errors.ApiErrorResponse;
import com.autosalone.exceptions.UnauthorizedException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {
    @Override
    public Response toResponse(UnauthorizedException exception) {
        ApiErrorResponse error = new ApiErrorResponse(401, exception.getMessage());
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}