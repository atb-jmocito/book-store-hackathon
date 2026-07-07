package com.bookstore.api;

import com.bookstore.config.RequestTracingFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnhandledExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnhandledExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        String traceId = RequestTracingFilter.currentTraceId();
        LOGGER.error("unhandled exception traceId={}", traceId, exception);

        ProblemDetails problem = ProblemDetails.from(
                "https://bookstore.dev/problems/internal-server-error",
                "Internal Server Error",
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Unexpected server error. Refer to trace id for support.",
                traceId);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.valueOf("application/problem+json"))
                .entity(problem)
                .build();
    }
}
