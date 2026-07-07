# OpenAPI docs

`openapi.yaml` is now source-of-truth contract for the runtime documentation endpoints:

- OpenAPI spec: `/api/v3/api-docs`
- OpenAPI YAML alias: `/api/openapi.yaml`
- Swagger UI: `/api/swagger-ui.html` in non-production environments

## Contract shape

- Primary endpoints follow the current compliant contract: `/health`, `/books`, and `/books/{id}`
- Legacy compatibility endpoints remain temporarily available and are marked as deprecated in the spec
- Error responses use RFC 7807 `application/problem+json`
- Authentication requirement today: none

## Maintenance notes

- The file is packaged into the application classpath and served at runtime
- Keep docs aligned with controller and service behavior when endpoints or status codes change
