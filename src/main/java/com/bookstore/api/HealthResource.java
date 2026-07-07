package com.bookstore.api;

import com.bookstore.config.RequestTracingFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {
    @GET
    public HealthStatus health() {
        return new HealthStatus("UP", RequestTracingFilter.currentTraceId());
    }

    public record HealthStatus(String status, String traceId) {
    }
}
