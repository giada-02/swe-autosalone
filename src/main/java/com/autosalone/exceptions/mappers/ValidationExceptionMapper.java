package com.autosalone.exceptions.mappers;

import com.autosalone.dtos.errors.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {

        String errorMessage = exception.getConstraintViolations().stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    String fieldName = path.substring(path.lastIndexOf('.') + 1);

                    return fieldName + " " + violation.getMessage();
                })
                .collect(Collectors.joining(", "));

        ApiErrorResponse error = new ApiErrorResponse(400, errorMessage);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}