package com.bookstore.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestTracingFilterTest {
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void requestFilterGeneratesTraceIdWhenHeaderMissing() throws Exception {
        RequestTracingFilter filter = new RequestTracingFilter();
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaderString(RequestTracingFilter.TRACE_ID_HEADER)).thenReturn(null);

        filter.filter(requestContext);

        assertNotEquals("unknown-trace", RequestTracingFilter.currentTraceId());
        verify(requestContext).setProperty(anyString(), anyString());
    }

    @Test
    void requestFilterSanitizesIncomingTraceId() throws Exception {
        RequestTracingFilter filter = new RequestTracingFilter();
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaderString(RequestTracingFilter.TRACE_ID_HEADER)).thenReturn(" trace id!* ");

        filter.filter(requestContext);

        assertEquals("traceid", RequestTracingFilter.currentTraceId());
    }

    @Test
    void responseFilterAddsTraceHeaderAndClearsMdc() throws Exception {
        RequestTracingFilter filter = new RequestTracingFilter();
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();

        when(requestContext.getProperty(RequestTracingFilter.TRACE_ID_PROPERTY)).thenReturn("trace-42");
        when(requestContext.getProperty(RequestTracingFilter.class.getName() + ".requestStartNanos")).thenReturn(System.nanoTime() - 2_000_000L);
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("books");
        when(responseContext.getStatus()).thenReturn(200);
        when(responseContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext, responseContext);

        assertEquals("trace-42", headers.getFirst(RequestTracingFilter.TRACE_ID_HEADER));
        assertEquals("unknown-trace", RequestTracingFilter.currentTraceId());
    }

    @Test
    void traceIdFallsBackToCurrentMdcValue() {
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getProperty(RequestTracingFilter.TRACE_ID_PROPERTY)).thenReturn(null);
        MDC.put("traceId", "trace-mdc");

        assertEquals("trace-mdc", RequestTracingFilter.traceId(requestContext));
    }

    @Test
    void responseFilterHandlesMissingStartTime() throws Exception {
        RequestTracingFilter filter = new RequestTracingFilter();
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();

        when(requestContext.getProperty(RequestTracingFilter.TRACE_ID_PROPERTY)).thenReturn("trace-99");
        when(requestContext.getProperty(RequestTracingFilter.class.getName() + ".requestStartNanos")).thenReturn(null);
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("books");
        when(responseContext.getStatus()).thenReturn(201);
        when(responseContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext, responseContext);

        assertTrue(headers.containsKey(RequestTracingFilter.TRACE_ID_HEADER));
    }
}
