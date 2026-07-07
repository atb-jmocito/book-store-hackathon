package com.bookstore.api;

import com.bookstore.config.RequestTracingFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ApiExceptionMapper implements ExceptionMapper<ApiException> {
    @Override
    public Response toResponse(ApiException exception) {
        ProblemDetails problem = ProblemDetails.from(
                exception.type(),
                exception.title(),
                exception.status().getStatusCode(),
                exception.getMessage(),
                RequestTracingFilter.currentTraceId());

        return Response.status(exception.status())
                .type(MediaType.valueOf("application/problem+json"))
                .entity(problem)
                .build();
    }
}
