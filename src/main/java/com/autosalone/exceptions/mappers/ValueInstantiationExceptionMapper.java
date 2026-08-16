package com.autosalone.exceptions.mappers;

import com.autosalone.dtos.errors.ApiErrorResponse;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ValueInstantiationExceptionMapper implements ExceptionMapper<ValueInstantiationException> {
    @Override
    public Response toResponse(ValueInstantiationException exception) {
        Throwable cause = exception.getCause();

        String message = (cause != null && cause.getMessage() != null)
                ? cause.getMessage()
                : "Invalid data format";

        ApiErrorResponse error = new ApiErrorResponse(400, message);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}