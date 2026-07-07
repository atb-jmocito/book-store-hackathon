package com.bookstore.api;

import com.bookstore.config.AppConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Path("/")
public class DocumentationResource {
    private final String openApiSpec;
    private final String apiBasePath;

    public DocumentationResource(AppConfig appConfig) {
        this.apiBasePath = appConfig.apiBasePathWithoutTrailingSlash();
        this.openApiSpec = loadOpenApiTemplate().replace("__API_BASE_PATH__", apiBasePath);
    }

    @GET
    @Path("/v3/api-docs")
    @Produces("application/yaml")
    public Response openApi() {
        return Response.ok(openApiSpec)
                .type("application/yaml")
                .cacheControl(noStore())
                .build();
    }

    @GET
    @Path("/openapi.yaml")
    @Produces("application/yaml")
    public Response openApiYaml() {
        return Response.ok(openApiSpec)
                .type("application/yaml")
                .cacheControl(noStore())
                .build();
    }

    @GET
    @Path("/swagger-ui.html")
    @Produces(MediaType.TEXT_HTML)
    public Response swaggerUi() {
        return Response.ok("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <title>Bookstore API Docs</title>
                  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui.css">
                </head>
                <body>
                  <div id="swagger-ui"></div>
                  <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                  <script>
                    window.onload = function () {
                      SwaggerUIBundle({
                        url: '%s/v3/api-docs',
                        dom_id: '#swagger-ui'
                      });
                    };
                  </script>
                </body>
                </html>
                """.formatted(apiBasePath))
                .cacheControl(noStore())
                .build();
    }

    private static CacheControl noStore() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoStore(true);
        return cacheControl;
    }

    private static String loadOpenApiTemplate() {
        try (InputStream inputStream = DocumentationResource.class.getClassLoader().getResourceAsStream("openapi/openapi.yaml")) {
            if (inputStream == null) {
                throw new IllegalStateException("OpenAPI resource openapi/openapi.yaml not found on classpath");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load OpenAPI specification", exception);
        }
    }
}
