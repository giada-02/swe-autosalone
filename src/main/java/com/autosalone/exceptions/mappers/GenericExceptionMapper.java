package com.autosalone.exceptions.mappers;

import com.autosalone.dtos.errors.ApiErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        exception.printStackTrace();

        ApiErrorResponse error = new ApiErrorResponse(500, "Internal Server Error. Please contact support.");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}