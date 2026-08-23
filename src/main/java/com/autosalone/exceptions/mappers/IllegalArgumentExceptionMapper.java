package com.autosalone.exceptions.mappers;

import com.autosalone.dtos.errors.ApiErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
    @Override
    public Response toResponse(IllegalArgumentException exception) {
        ApiErrorResponse error = new ApiErrorResponse(400, exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}