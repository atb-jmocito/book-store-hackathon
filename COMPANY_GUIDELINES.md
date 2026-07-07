Company Java API Standards
1. Supported Java Versions
To ensure security patches, performance improvements, and ecosystem compatibility, we strictly adhere to Long-Term Support (LTS) releases.

Default Version: Java 21 (LTS) is the default for all new projects.

Allowed Legacy Version: Java 17 (LTS) is permitted for existing services, though teams should plan upgrades to 21.

Prohibited: Non-LTS versions (e.g., 18, 19, 20, 22) and End-of-Life versions (e.g., 8, 11) are strictly prohibited for production deployments.

Build Tools: Maven or Gradle are the only permitted build tools. Maven is preferred for cross-team consistency.

2. API Specification (OpenAPI)
We follow a design-first (or strictly documented code-first) approach to API development.

Requirement: Every API must expose an OpenAPI 3.0+ (formerly Swagger) specification.

Accessibility: The spec must be accessible via a standard endpoint (e.g., /v3/api-docs or /openapi.json) and include a visual UI (e.g., Swagger UI at /swagger-ui.html) in non-production environments.

Completeness: The specification must include comprehensive descriptions, request/response payloads, authentication requirements, and all possible HTTP status codes for every endpoint.

3. Naming Conventions
Consistency in naming reduces cognitive load for developers and API consumers.

REST Endpoints (URIs)

Use kebab-case for all URIs (e.g., /user-profiles, not /userProfiles).

Use plural nouns for resources (e.g., /api/v1/users, not /api/v1/user).

Avoid using verbs in the URI; use HTTP methods instead (e.g., POST /users, not POST /create-user).

Java Codebase

Packages: strictly lowercase, reverse domain notation (e.g., com.company.service.users).

Classes/Interfaces: PascalCase (e.g., UserController, PaymentService).

Methods/Variables: camelCase (e.g., getUserById, customerName).

Constants: UPPER_SNAKE_CASE (e.g., MAX_RETRY_COUNT).

4. README Guidelines
Every repository must contain a README.md at the root. Think of the README as the front door to your service. It must include the following sections:

Project Title & Description: What the service does and its domain context.

Prerequisites: Required software (e.g., Java 21, Docker, specific environment variables).

Getting Started: Step-by-step instructions to build and run the service locally.

API Documentation Link: Where to find the local and staging OpenAPI/Swagger UI.

Testing Setup: How to execute unit and integration tests.

Architecture/Design Notes: Brief overview of key design decisions, dependencies (e.g., Postgres, Redis), and downstream systems.

Maintainers: Code owners or Slack channel for support.

5. Error Handling
APIs must never return raw stack traces to the client.

Standard Format: All errors must follow RFC 7807 (Problem Details for HTTP APIs).

Structure: Error responses must include a type, title, status, detail, and instance (trace ID).

HTTP Status Codes: Strictly adhere to standard HTTP codes (e.g., 200 for OK, 400 for Bad Request, 401 for Unauthorized, 403 for Forbidden, 404 for Not Found, 500 for Internal Server Error).

6. Logging and Observability
Format: Logs must be output in JSON format to ensure compatibility with our log aggregation tools (e.g., ELK, Datadog).

Context: Every request must generate a unique Trace ID (e.g., via MDC in Spring Boot) that is injected into all log entries and passed to downstream services via headers.

Levels: Use ERROR for system failures requiring intervention, WARN for handled exceptions/retries, INFO for lifecycle events, and DEBUG for troubleshooting (disabled in production).

7. Testing Requirements
Minimum Coverage: All APIs require a minimum of 70% line coverage for business logic.

Test Types: Projects must include both Unit Tests (mocked dependencies) and Integration Tests (testing the web layer and database integrations, ideally using Testcontainers).