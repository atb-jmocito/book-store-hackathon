# Technical documentation

## Purpose

This document gives one combined high-level and low-level view of the Bookstore service. It is meant for technical readers who need to understand what the service does, how it is wired, where data flows, and which parts of the codebase to change when extending behavior.

## System summary

Bookstore is a small Java service that exposes a JSON HTTP API for managing book inventory. The service runs as a standalone Grizzly server with Jersey resources and stores book records in MongoDB.

At repo level, the system has two runtime processes in local development:

1. `api` - Java application packaged as a shaded JAR
2. `mongodb` - MongoDB 7 container with a small seed script for categories

## Architecture overview

### Runtime components

| Component | Responsibility | Main files |
| --- | --- | --- |
| Grizzly HTTP server | Starts listener on port `8080` and mounts Jersey application under `/api` | `src/main/java/com/bookstore/BookstoreApp.java` |
| Jersey resource layer | Maps HTTP requests to controller methods | `src/main/java/com/bookstore/controller/BookController.java` |
| Service layer | Contains query/update logic and MongoDB access | `src/main/java/com/bookstore/service/BookService.java` |
| Domain and DTO classes | Represent response payloads and request bodies | `src/main/java/com/bookstore/model/Book.java`, `src/main/java/com/bookstore/model/Category.java`, `src/main/java/com/bookstore/controller/CreateBookDTO.java`, `src/main/java/com/bookstore/controller/UpdateBookDTO.java` |
| MongoDB | Persists `books` collection and seeds `categories` collection | `docker-compose.yml`, `mongo-init/01-init-bookstore.js` |

### Logical flow

```text
Client
  -> HTTP request /api/books...
  -> BookController
  -> BookService
  -> MongoDB driver
  -> MongoDB database
  -> BookService maps Document -> Book
  -> BookController returns JSON response
```

### Boundaries

- HTTP boundary: JSON over REST-like endpoints under `/api/books`
- Persistence boundary: synchronous MongoDB driver calls inside `BookService`
- Deployment boundary: service and database run as separate containers in Docker Compose

## Integrations

### MongoDB

- Database name: `bookstore`
- Main collection used by the API: `books`
- Seeded collection: `categories`
- Connection target in code: `mongodb://mongodb:27017`

MongoDB is the only operational integration in the current codebase. `BookService` creates a new client per call, reads/writes directly with `Document`, and manually maps values into the `Book` model.

### Docker Compose

`docker-compose.yml` defines the local runtime topology:

- `api` service builds from the repo `Dockerfile`
- `mongodb` service uses `mongo:7.0`
- `api` depends on `mongodb`
- `api` sets `MONGODB_URI=mongodb://mongodb:27017`, but the current Java code does not consume this environment variable
- ports are exposed as `8080:8080` and `27017:27017`

This makes the expected local entrypoints:

- API base URL: `http://localhost:8080/api`
- MongoDB: `mongodb://localhost:27017`

### OpenAPI documentation

API contract documentation lives separately under `docs/openapi/`:

- Spec file: `docs/openapi/openapi.yaml`
- Notes: `docs/openapi/README.md`

This technical doc references that folder for endpoint-by-endpoint contract detail and focuses instead on runtime structure and implementation.

## Request and data flow

### Application startup

`BookstoreApp` is the bootstrap class. It loads environment-driven configuration, creates a shared MongoDB client, registers JSON binding, request tracing, RFC 7807 exception mappers, runtime documentation resources, and the `BookController`, then starts Grizzly on the configured base URI.

Implications:

- runtime host, port, API base path, MongoDB URI, and Swagger UI exposure are externally configurable
- request tracing and JSON logging are applied globally
- shutdown closes both the HTTP server and shared MongoDB client

### Read flow

For `GET /api/books`:

1. `BookController.listBooks(...)` receives optional `category`
2. `BookService.listBooks(...)` resolves the category name against static in-memory categories
3. service queries MongoDB directly, optionally filtering by category id
5. controller returns `200 OK` with JSON array

For `GET /api/books/single`:

1. controller accepts `id`, `name`, and `author`
2. controller delegates to `BookService.getSingleBook(...)`
3. service supports lookup by id, exact title, or exact author
4. errors are returned as RFC 7807 problem responses instead of uncaught exceptions or `null` payloads

### Write flow

For `POST /api/books`:

1. controller accepts `CreateBookDTO`
2. request is validated for required title and supported category
3. controller forwards request data to `BookService.createBook(...)`
4. service converts category name to numeric category id
5. service inserts a MongoDB document into `books`
6. controller returns `201 Created` with the persisted `Book`

For `PUT /api/books`:

1. controller accepts `UpdateBookDTO`
2. request is considered valid only when `id` is non-null
3. service updates only `quantity`, `active`, and `inactiveDate`
4. controller returns `201 Created` with a sparse in-memory `Book`

## Project structure

```text
.
├── docs/
│   ├── openapi/
│   │   ├── README.md
│   │   └── openapi.yaml
│   └── technical/
│       └── README.md
├── mongo-init/
│   └── 01-init-bookstore.js
├── src/
│   ├── main/
│   │   └── java/com/bookstore/
│   │       ├── BookstoreApp.java
│   │       ├── controller/
│   │       │   ├── BookController.java
│   │       │   ├── CreateBookDTO.java
│   │       │   └── UpdateBookDTO.java
│   │       ├── model/
│   │       │   ├── Book.java
│   │       │   └── Category.java
│   │       └── service/
│   │           └── BookService.java
│   └── test/
│       └── java/com/bookstore/controller/
│           └── BookControllerTest.java
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.MD
```

### Package responsibilities

#### `com.bookstore`

- application bootstrap only

#### `com.bookstore.controller`

- HTTP entrypoints
- JAX-RS annotations
- HTTP entrypoints, compatibility routes, and DTOs used for request deserialization
- DTOs used for request deserialization

#### `com.bookstore.service`

- business and persistence logic are still combined here
- category resolution, validation, document persistence, and result mapping live in one class

#### `com.bookstore.model`

- simple mutable POJOs for API payloads and static category definitions

#### `src/test`

- contains controller tests, service tests, and Docker-gated integration tests

## Data model

### Book

`Book` is the main domain object returned by the API. It contains:

- identity: `id`
- inventory: `quantity`, `active`, `inactiveDate`
- bibliographic fields: `title`, `author`, `publisher`, `publisherDate`, `language`, `description`
- classification: numeric `categoryId`

### Category

Categories are defined twice:

- statically in Java via `Category.STATIC_CATEGORIES`
- in MongoDB seed script via `mongo-init/01-init-bookstore.js`

Current categories:

- Fiction
- Non-Fiction
- Science
- Biography
- Children
- Programming

## Build, run, and packaging

### Build toolchain

- Maven project
- Java release target: 21
- shaded executable JAR produced by `maven-shade-plugin`

### Key dependencies

| Dependency | Purpose |
| --- | --- |
| `jakarta.ws.rs-api` | JAX-RS annotations and API |
| `jersey-container-grizzly2-http` | Embedded HTTP server integration |
| `jersey-hk2` | Jersey injection support |
| `jersey-media-json-binding` | JSON serialization/deserialization |
| `mongodb-driver-sync` | Synchronous MongoDB access |
| `slf4j`, `logback`, `logstash-logback-encoder` | JSON logging with MDC trace ids |
| `junit-jupiter`, `mockito`, `testcontainers` | Unit and integration testing |

### Local run path

Preferred local path in current repo is:

```bash
docker-compose up
```

The Docker image is built in two stages:

1. Maven build stage compiles and packages the JAR
2. Eclipse Temurin JRE stage runs `app.jar`

## Known technical gaps and risks

These are implementation observations, not proposed fixes:

- categories are still defined in both Java and MongoDB seed data, which creates duplication risk
- there is still no authentication or authorization model
- legacy compatibility routes remain exposed during transition and should eventually be removed

## Extension points

Readers changing the service will usually start here:

- add or change endpoints: `src/main/java/com/bookstore/controller/BookController.java`
- change persistence rules or query behavior: `src/main/java/com/bookstore/service/BookService.java`
- change payload shape: DTOs in `controller/` and models in `model/`
- change categories: `src/main/java/com/bookstore/model/Category.java` and `mongo-init/01-init-bookstore.js`
- change container/runtime behavior: `Dockerfile` and `docker-compose.yml`

## Related documents

- API contract: `../openapi/openapi.yaml`
- OpenAPI notes: `../openapi/README.md`
- Repository entrypoint: `../../README.MD`
