package com.bookstore.api;

import com.bookstore.config.AppConfig;
import com.bookstore.config.RequestTracingFilter;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiComponentsTest {
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void apiExceptionMapperBuildsProblemDetailsResponse() {
        MDC.put("traceId", "trace-123");
        ApiExceptionMapper mapper = new ApiExceptionMapper();
        ApiException exception = ApiException.badRequest("Field 'title' is required.");

        Response response = mapper.toResponse(exception);
        ProblemDetails problemDetails = (ProblemDetails) response.getEntity();

        assertEquals(400, response.getStatus());
        assertEquals("application/problem+json", response.getMediaType().toString());
        assertEquals("Bad Request", problemDetails.title());
        assertEquals("urn:trace:trace-123", problemDetails.instance());
    }

    @Test
    void unhandledExceptionMapperBuildsInternalServerErrorResponse() {
        MDC.put("traceId", "trace-500");
        UnhandledExceptionMapper mapper = new UnhandledExceptionMapper();

        Response response = mapper.toResponse(new IllegalStateException("boom"));
        ProblemDetails problemDetails = (ProblemDetails) response.getEntity();

        assertEquals(500, response.getStatus());
        assertEquals("Internal Server Error", problemDetails.title());
        assertEquals("urn:trace:trace-500", problemDetails.instance());
    }

    @Test
    void healthResourceReturnsTraceAwareStatus() {
        MDC.put("traceId", "trace-health");
        HealthResource resource = new HealthResource();

        HealthResource.HealthStatus status = resource.health();

        assertEquals("UP", status.status());
        assertEquals("trace-health", status.traceId());
    }

    @Test
    void problemDetailsFactoryPrefixesTraceInstance() {
        ProblemDetails problemDetails = ProblemDetails.from("type", "title", 400, "detail", "trace-x");

        assertEquals("urn:trace:trace-x", problemDetails.instance());
        assertEquals("detail", problemDetails.detail());
    }

    @Test
    void documentationResourceServesOpenApiAndSwaggerUi() {
        AppConfig appConfig = new AppConfig("127.0.0.1", 8080, "/api", "mongodb://localhost:27017", "bookstore", "books", "local", true);
        DocumentationResource resource = new DocumentationResource(appConfig);

        Response openApi = resource.openApi();
        Response openApiYaml = resource.openApiYaml();
        Response swaggerUi = resource.swaggerUi();

        String openApiBody = (String) openApi.getEntity();
        String swaggerBody = (String) swaggerUi.getEntity();

        assertEquals(200, openApi.getStatus());
        assertEquals("application/yaml", openApi.getMediaType().toString());
        assertTrue(openApiBody.contains("/books/{id}:"));
        assertEquals(openApiBody, openApiYaml.getEntity());
        assertTrue(swaggerBody.contains("/api/v3/api-docs"));
        assertTrue(swaggerBody.contains("SwaggerUIBundle"));
    }
}
