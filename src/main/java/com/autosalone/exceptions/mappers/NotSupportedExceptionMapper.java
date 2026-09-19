package com.autosalone.exceptions.mappers;

import com.autosalone.dtos.errors.ApiErrorResponse;

import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotSupportedExceptionMapper implements ExceptionMapper<NotSupportedException> {
    @Override
    public Response toResponse(NotSupportedException exception) {
        ApiErrorResponse error = new ApiErrorResponse(415, exception.getMessage());
        return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}