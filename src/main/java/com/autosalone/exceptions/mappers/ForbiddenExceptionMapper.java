package com.autosalone.exceptions.mappers;

import com.autosalone.dtos.errors.ApiErrorResponse;
import com.autosalone.exceptions.ForbiddenException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {
    @Override
    public Response toResponse(ForbiddenException exception) {
        ApiErrorResponse error = new ApiErrorResponse(403, exception.getMessage());
        return Response.status(Response.Status.FORBIDDEN)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}