# Changelog

## 2026-07-07 - FR2 author management and book-author linkage

### Summary

This change set implements feature request #2 by introducing first-class author management, canonical book-to-author linkage, author-based book filtering, and a documentation-first feature pack under `docs/features/fr2/`. It also refreshes shared repository docs and the runtime OpenAPI contract so the shipped behavior stays fully documented.

### Why this change exists

- Replace free-text-only author handling with first-class author entities
- Support author lifecycle management without breaking current book clients
- Enable stable author-based filtering across the catalog
- Keep feature specification, implementation tasks, QA checklist, and shared docs aligned with delivered behavior

### API changes

- Added author management endpoints:
  - `POST /api/authors`
  - `GET /api/authors`
  - `GET /api/authors/{id}`
  - `DELETE /api/authors/{id}` for soft-delete
- Extended `GET /api/books` with optional `authorId` filtering
- Extended `POST /api/books` with preferred `authorId` input
- Preserved legacy `author` request/response behavior for compatibility
- Kept deprecated compatibility route `GET /api/books/single` working for author lookups by keeping author display text on books

### Domain and persistence changes

- Added new `Author` model and `CreateAuthorDTO`
- Added dedicated `AuthorService` for validation, persistence, uniqueness, soft-delete, and canonical author resolution
- Added dedicated `AuthorController`
- Extended `Book` with additive `authorId`
- Updated `BookService` to:
  - resolve books against existing active authors
  - auto-create minimal author records when legacy book creation provides only an unknown author name
  - filter books directly by `authorId`
- Added MongoDB author uniqueness support via `authors.nameKey`
- Added MongoDB index support for `books.authorId`
- Added configurable author collection name via `BOOKSTORE_AUTHOR_COLLECTION`

### Validation and behavior changes

- Added conflict responses for duplicate author names
- Added validation for invalid `authorId` values
- Prevented linking new books to inactive authors through the legacy name path
- Kept soft-deleted authors retrievable by id and list filters
- Kept linked books readable after author soft-delete

### Documentation changes

- Added feature-specific docs:
  - `docs/features/fr2/spec.md`
  - `docs/features/fr2/tasks.md`
  - `docs/features/fr2/checklist.md`
- Updated `docs/openapi/openapi.yaml` for author endpoints and book author linkage
- Updated `docs/openapi/README.md`
- Updated `docs/functional/README.md`
- Updated `docs/technical/README.md`
- Updated root `README.md` for author support and new configuration surface

### Test coverage changes

- Added unit tests for:
  - `AuthorController`
  - `AuthorService`
- Expanded tests for:
  - `BookController`
  - `BookService`
  - `CreateBookDTO`
  - API documentation components
  - end-to-end integration behavior for author lifecycle and author-linked books

### Pull request notes

Suggested reviewer focus:

1. **API:** author lifecycle routes, book `authorId` behavior, compatibility handling
2. **Persistence:** canonical author resolution, soft-delete semantics, Mongo indexes
3. **Docs and quality:** OpenAPI alignment, feature docs, expanded tests

Behavioral considerations for reviewers:

- `authorId` is additive; `author` remains in book payloads for compatibility
- `DELETE /authors/{id}` performs soft-delete, not hard-delete
- new books may implicitly create a minimal author record when only legacy `author` text is supplied
- author updates are still out of scope for this feature

## 2026-07-07 - Company guideline compliance refactor

### Summary

This change set refactors the Bookstore service to align the repository and runtime behavior with `COMPANY_GUIDELINES.md`. It upgrades the platform to Java 21, normalizes the API contract, adds standardized error handling and observability, exposes runtime OpenAPI documentation, improves MongoDB access patterns, refreshes repository documentation, and raises automated test coverage above the required 70% threshold.

### Why this change exists

- Bring the project onto a supported Java LTS baseline
- Make runtime API behavior match documented company standards
- Replace surprising or unsafe runtime behavior with explicit validation and RFC 7807 problem responses
- Add traceable, structured logging and request correlation
- Make OpenAPI available from standard runtime endpoints
- Increase test confidence and enforce a minimum coverage threshold for future changes

### Platform and runtime changes

- Upgraded build/runtime target from Java 11 to **Java 21**
- Updated Maven plugins and dependencies to support Java 21, structured logging, and modern testing
- Updated Docker build and runtime images to Java 21
- Hardened container runtime by running the application as a non-root user
- Externalized runtime configuration into `AppConfig` using environment variables for:
  - host
  - port
  - API base path
  - MongoDB URI
  - database and collection names
  - environment / Swagger UI toggle
- Added graceful shutdown for the HTTP server and shared MongoDB client

### API contract and backward-compatibility changes

- Kept compatibility constraint: preserve legacy client behavior during transition where practical
- Added/normalized primary runtime endpoints:
  - `GET /api/health`
  - `GET /api/books`
  - `POST /api/books`
  - `GET /api/books/{id}`
  - `PUT /api/books/{id}`
- Kept deprecated compatibility routes with deprecation headers:
  - `GET /api/books/health`
  - `GET /api/books/single`
  - `PUT /api/books`
- Fixed controller naming and DTO inconsistencies
- Introduced preferred `categoryName` for create requests while still accepting legacy `categoryId` alias during transition
- Changed update flow to return the refreshed full `Book` entity instead of sparse in-memory data
- Added author-based lookup support on the legacy single-book compatibility route

### Error handling and validation changes

- Added centralized API exception model:
  - `ApiException`
  - `ApiExceptionMapper`
  - `UnhandledExceptionMapper`
  - `ProblemDetails`
- Standardized error responses to **RFC 7807** `application/problem+json`
- Replaced raw/unhandled runtime failures with explicit bad-request, not-found, or internal-server-error responses
- Corrected invalid status behaviors such as validation-like failures returning `404`
- Added validation for:
  - missing request body
  - missing required title
  - invalid category values
  - invalid or missing MongoDB ObjectId values
  - empty update payloads

### Observability and logging changes

- Added `RequestTracingFilter` for per-request trace IDs
- Reads inbound `X-Trace-Id` when present and generates one when absent
- Returns trace ID in response headers
- Injects trace ID into logging context via MDC
- Replaced console logging with JSON logging using Logback + logstash encoder
- Included trace ID in problem response `instance` values

### OpenAPI and documentation changes

- Added runtime documentation resource serving:
  - `GET /api/v3/api-docs`
  - `GET /api/openapi.yaml`
  - `GET /api/swagger-ui.html`
- Reworked `docs/openapi/openapi.yaml` to reflect the new preferred contract plus deprecated compatibility routes
- Updated `docs/openapi/README.md` for runtime docs endpoints
- Updated technical and functional docs to reflect:
  - Java 21 runtime
  - new endpoint behavior
  - RFC 7807 errors
  - trace IDs and JSON logging
  - compatibility route strategy
- Renamed root README from `README.MD` to **`README.md`**
- Replaced root README content with guideline-compliant sections:
  - project description
  - prerequisites
  - getting started
  - API documentation links
  - testing setup
  - architecture notes
  - maintainers

### Service and persistence refactors

- Reworked `BookService` to use a shared MongoDB collection instead of creating a new client per call
- Replaced full-scan/filter-in-memory behavior with direct MongoDB query filters where appropriate
- Fixed ID handling by using proper `ObjectId` parsing and value-based lookup
- Added explicit category resolution via `Category.findByName(...)`
- Normalized create and update flows around validated request data
- Ensured update operations mutate only intended fields and re-read the persisted document for the response
- Kept static category model but reduced lookup duplication through a normalized category map

### Test coverage improvements

- Added/expanded unit tests for:
  - `BookController`
  - `BookService`
  - `CreateBookDTO`
  - API exception/documentation/health components
  - request tracing filter behavior
- Added Docker-gated integration tests for:
  - runtime API behavior
  - OpenAPI endpoint availability
  - legacy compatibility routes
  - RFC 7807 error responses
- Enabled JaCoCo coverage checks in Maven
- Expanded coverage enforcement from service-only scope to **bundle-wide scope**
- Final automated coverage result after this change: **77.3% line coverage**

### Verification

Use Java 21 when validating locally:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn verify
```

### Pull request notes

Suggested PR focus:

1. **Compliance:** Java 21, README/doc updates, runtime OpenAPI, RFC 7807 errors, JSON logging, trace IDs
2. **Refactor:** `BookService` data access cleanup, endpoint normalization, DTO cleanup, compatibility routing
3. **Quality:** new unit/integration tests and bundle-wide JaCoCo enforcement at 70% minimum

Behavioral considerations for reviewers:

- New preferred endpoints exist alongside deprecated compatibility endpoints
- Error payloads are now standardized and no longer expose raw runtime exceptions
- Swagger UI is intentionally available only in non-production environments via configuration
- Coverage enforcement is stricter than before and now applies to the broader application bundle
