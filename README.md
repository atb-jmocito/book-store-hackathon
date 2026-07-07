# Bookstore API

Bookstore API manages bookstore inventory records and author profiles backed by MongoDB. It exposes health, author management, book listing and lookup, book creation and update operations, plus runtime OpenAPI documentation.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker and Docker Compose
- MongoDB, unless running through Docker Compose
- Environment variables when overriding defaults: `MONGODB_URI`, `SERVER_HOST`, `SERVER_PORT`, `API_BASE_PATH`, `APP_ENV`, `ENABLE_SWAGGER_UI`, `BOOKSTORE_DATABASE`, `BOOKSTORE_COLLECTION`, `BOOKSTORE_AUTHOR_COLLECTION`

## Getting Started

### Run with Docker Compose

```bash
docker-compose up --build
```

### Run locally with Maven

```bash
export MONGODB_URI=mongodb://localhost:27017
mvn package
java -jar target/bookstore-java-0.1.0-SNAPSHOT.jar
```

Default local API base URL: `http://localhost:8080/api`

## API Documentation Link

- OpenAPI YAML: `http://localhost:8080/api/v3/api-docs`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- Source contract file: `docs/openapi/openapi.yaml`

## Testing Setup

```bash
mvn verify
```

The build runs unit tests, Docker-gated integration tests, and JaCoCo bundle-wide coverage checks with a 70% minimum threshold.

## Architecture / Design Notes

- Runtime stack: Jersey + Grizzly HTTP server, MongoDB Java driver, MongoDB 7
- Configuration is environment-driven through `AppConfig`
- Errors are returned as RFC 7807 problem details with trace ids
- Request logging is emitted as JSON and includes per-request trace ids
- Books now support canonical author linkage through first-class author records
- Legacy endpoints and legacy book author field remain temporarily available while clients migrate to the documented contract

## Maintainers

- Repository owner: `@atb-jmocito`
- Team support: update this section with the owning Slack channel or code owners group when available
