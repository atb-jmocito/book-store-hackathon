package com.bookstore.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

public class RequestTracingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_PROPERTY = RequestTracingFilter.class.getName() + ".traceId";
    private static final String REQUEST_START_NANOS = RequestTracingFilter.class.getName() + ".requestStartNanos";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTracingFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String traceId = sanitizeTraceId(requestContext.getHeaderString(TRACE_ID_HEADER));
        requestContext.setProperty(TRACE_ID_PROPERTY, traceId);
        requestContext.setProperty(REQUEST_START_NANOS, System.nanoTime());
        MDC.put("traceId", traceId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        String traceId = traceId(requestContext);
        responseContext.getHeaders().putSingle(TRACE_ID_HEADER, traceId);

        long durationMs = durationMs(requestContext);
        LOGGER.info(
                "request handled method={} path={} status={} durationMs={}",
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                responseContext.getStatus(),
                durationMs);

        MDC.remove("traceId");
    }

    public static String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? "unknown-trace" : traceId;
    }

    public static String traceId(ContainerRequestContext requestContext) {
        Object traceId = requestContext.getProperty(TRACE_ID_PROPERTY);
        return traceId == null ? currentTraceId() : traceId.toString();
    }

    private static String sanitizeTraceId(String incomingTraceId) {
        if (incomingTraceId == null || incomingTraceId.isBlank()) {
            return UUID.randomUUID().toString();
        }

        String sanitized = incomingTraceId.trim().replaceAll("[^A-Za-z0-9._-]", "");
        return sanitized.isBlank() ? UUID.randomUUID().toString() : sanitized.substring(0, Math.min(sanitized.length(), 64));
    }

    private static long durationMs(ContainerRequestContext requestContext) {
        Object started = requestContext.getProperty(REQUEST_START_NANOS);
        if (started instanceof Long startedAt) {
            return (System.nanoTime() - startedAt) / 1_000_000L;
        }
        return -1L;
    }
}
