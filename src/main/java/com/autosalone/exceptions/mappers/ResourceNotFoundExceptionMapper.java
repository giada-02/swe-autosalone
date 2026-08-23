package com.autosalone.exceptions.mappers;

import com.autosalone.dtos.errors.ApiErrorResponse;
import com.autosalone.exceptions.ResourceNotFoundException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {
    @Override
    public Response toResponse(ResourceNotFoundException exception) {
        ApiErrorResponse error = new ApiErrorResponse(404, exception.getMessage());
        return Response.status(Response.Status.NOT_FOUND)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}