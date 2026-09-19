package com.autosalone.exceptions.mappers;

import com.autosalone.dtos.errors.ApiErrorResponse;

import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotAllowedExceptionMapper implements ExceptionMapper<NotAllowedException> {
    @Override
    public Response toResponse(NotAllowedException exception) {
        ApiErrorResponse error = new ApiErrorResponse(405, exception.getMessage());
        return Response.status(Response.Status.METHOD_NOT_ALLOWED)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}