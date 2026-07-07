# Technical documentation

## Purpose

This document gives one combined high-level and low-level view of the Bookstore service after FR2 author support. It is meant for technical readers who need to understand runtime structure, data flow, and the main files to touch when extending behavior.

## System summary

Bookstore is a standalone Java service that exposes a JSON HTTP API for managing books and authors. The service runs on Grizzly with Jersey resources and stores data in MongoDB.

At repo level, local development still uses two runtime processes:

1. `api` - Java application packaged as a shaded JAR
2. `mongodb` - MongoDB 7 container with seed data for categories

## Architecture overview

### Runtime components

| Component | Responsibility | Main files |
| --- | --- | --- |
| Grizzly HTTP server | Starts listener on port `8080` and mounts Jersey application under `/api` | `src/main/java/com/bookstore/BookstoreApp.java` |
| Book resource layer | Maps HTTP requests for books to service methods | `src/main/java/com/bookstore/controller/BookController.java` |
| Author resource layer | Maps HTTP requests for authors to service methods | `src/main/java/com/bookstore/controller/AuthorController.java` |
| Book service layer | Book query/update logic plus author-linked create flow | `src/main/java/com/bookstore/service/BookService.java` |
| Author service layer | Author validation, persistence, soft-delete, and canonical author resolution | `src/main/java/com/bookstore/service/AuthorService.java` |
| Domain and DTO classes | Represent request/response payloads | `src/main/java/com/bookstore/model/Book.java`, `src/main/java/com/bookstore/model/Author.java`, `src/main/java/com/bookstore/controller/CreateBookDTO.java`, `src/main/java/com/bookstore/controller/CreateAuthorDTO.java`, `src/main/java/com/bookstore/controller/UpdateBookDTO.java` |
| MongoDB | Persists `books` and `authors` collections; seeds `categories` collection | `docker-compose.yml`, `mongo-init/01-init-bookstore.js` |

### Logical flow

```text
Client
  -> HTTP request /api/books... or /api/authors...
  -> BookController / AuthorController
  -> BookService / AuthorService
  -> MongoDB driver
  -> MongoDB database
  -> service maps Document -> model
  -> controller returns JSON response
```

### Boundaries

- HTTP boundary: JSON over REST-like endpoints under `/api`
- Persistence boundary: synchronous MongoDB driver calls inside service classes
- Deployment boundary: service and database run as separate containers in Docker Compose

## Integrations

### MongoDB

- Database name: `bookstore`
- Main collections used by the API: `books`, `authors`
- Seeded collection: `categories`
- Default connection target in code: `mongodb://mongodb:27017`

Current persistence split:

- `BookService` reads and writes `books`
- `AuthorService` reads and writes `authors`
- category validation still relies on static in-memory definitions

Author-related persistence notes:

- `authors.nameKey` unique index enforces case-insensitive author uniqueness
- `books.authorId` index supports author-based filtering
- book documents store both `authorId` and legacy-compatible `author` display text

### Docker Compose

`docker-compose.yml` defines the local runtime topology:

- `api` service builds from repo `Dockerfile`
- `mongodb` service uses `mongo:7.0`
- `api` depends on `mongodb`
- `api` sets `MONGODB_URI=mongodb://mongodb:27017`

Expected local entrypoints:

- API base URL: `http://localhost:8080/api`
- MongoDB: `mongodb://localhost:27017`

### OpenAPI documentation

API contract documentation lives under `docs/openapi/`:

- Spec file: `docs/openapi/openapi.yaml`
- Notes: `docs/openapi/README.md`

## Request and data flow

### Application startup

`BookstoreApp` loads environment-driven configuration, creates one shared MongoDB client, builds `AuthorService` and `BookService`, registers JSON binding, request tracing, RFC 7807 exception mappers, runtime documentation resources, and the book/author controllers, then starts Grizzly on the configured base URI.

Implications:

- runtime host, port, API base path, MongoDB URI, database name, book collection name, author collection name, and Swagger UI exposure are externally configurable
- request tracing and JSON logging are applied globally
- missing MongoDB indexes are created by service constructors
- shutdown closes both the HTTP server and shared MongoDB client

### Book read flow

For `GET /api/books`:

1. `BookController.listBooks(...)` receives optional `category` and `authorId`
2. `BookService.listBooks(...)` validates category name and author id format when supplied
3. service queries MongoDB directly using category filter, author filter, or both
4. controller returns `200 OK` with JSON array

For single-book reads:

1. primary route is `GET /api/books/{id}`
2. deprecated compatibility route `GET /api/books/single` accepts `id`, `name`, and `author`
3. controller delegates to `BookService.getBookById(...)` or `BookService.getSingleBook(...)`
4. service supports lookup by id, exact title, or exact author display text
5. errors are returned as RFC 7807 problem responses

### Book write flow

For `POST /api/books`:

1. controller accepts `CreateBookDTO`
2. request is validated for required title and supported category
3. service resolves canonical author link:
   - direct `authorId`
   - existing active author by legacy `author` text
   - or minimal auto-created author when legacy text has no match
4. service inserts a MongoDB document into `books`
5. controller returns `201 Created` with persisted `Book`

For `PUT /api/books/{id}`:

1. primary route accepts path parameter `id` plus `UpdateBookDTO`
2. deprecated compatibility route `PUT /api/books` still accepts `UpdateBookDTO.id`
3. service updates only `quantity`, `active`, and `inactiveDate`
4. controller returns `200 OK` with refreshed persisted `Book`

### Author flow

For `POST /api/authors`:

1. controller accepts `CreateAuthorDTO`
2. service validates required `name`, optional `email`, and duplicate name rules
3. service inserts active author document with timestamps
4. controller returns `201 Created`

For `GET /api/authors`:

1. controller accepts optional `active`
2. service queries all authors or filtered subset
3. controller returns `200 OK` with JSON array

For `GET /api/authors/{id}`:

1. controller validates through service
2. service loads author by MongoDB ObjectId
3. inactive authors remain readable

For `DELETE /api/authors/{id}`:

1. service validates id and loads existing author
2. active author is updated to `active=false` with fresh `updatedAt`
3. controller returns updated author payload

## Project structure

```text
.
├── docs/
│   ├── features/
│   │   └── fr2/
│   │       ├── spec.md
│   │       ├── tasks.md
│   │       └── checklist.md
│   ├── openapi/
│   │   ├── README.md
│   │   └── openapi.yaml
│   ├── technical/
│   │   └── README.md
│   └── functional/
│       └── README.md
├── mongo-init/
│   └── 01-init-bookstore.js
├── src/
│   ├── main/
│   │   └── java/com/bookstore/
│   │       ├── BookstoreApp.java
│   │       ├── api/
│   │       ├── config/
│   │       ├── controller/
│   │       │   ├── AuthorController.java
│   │       │   ├── BookController.java
│   │       │   ├── CreateAuthorDTO.java
│   │       │   ├── CreateBookDTO.java
│   │       │   └── UpdateBookDTO.java
│   │       ├── model/
│   │       │   ├── Author.java
│   │       │   ├── Book.java
│   │       │   └── Category.java
│   │       └── service/
│   │           ├── AuthorService.java
│   │           └── BookService.java
│   └── test/
│       └── java/com/bookstore/
│           ├── api/
│           ├── controller/
│           ├── integration/
│           └── service/
├── Dockerfile
├── docker-compose.yml
├── CHANGELOG.md
├── pom.xml
└── README.md
```

## Data model

### Author

`Author` is a first-class API object. It contains:

- identity: `id`
- profile fields: `name`, `birthDate`, `nationality`, `email`
- lifecycle: `active`, `createdAt`, `updatedAt`

### Book

`Book` remains the main inventory object. It now contains:

- identity: `id`
- bibliographic fields: `title`, `authorId`, `author`, `publisher`, `publisherDate`, `language`, `description`
- inventory: `quantity`, `active`, `inactiveDate`
- classification: numeric `categoryId`

### Category

Categories are still defined twice:

- statically in Java via `Category.STATIC_CATEGORIES`
- in MongoDB seed script via `mongo-init/01-init-bookstore.js`

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

Preferred local path:

```bash
docker-compose up
```

## Known technical gaps and risks

- categories are still defined in both Java and MongoDB seed data, which creates duplication risk
- there is still no authentication or authorization model
- legacy compatibility routes remain exposed during transition
- authors have no update flow yet, so profile corrections require future work

## Extension points

- add or change author endpoints: `src/main/java/com/bookstore/controller/AuthorController.java`
- add or change book endpoints: `src/main/java/com/bookstore/controller/BookController.java`
- change author lifecycle rules: `src/main/java/com/bookstore/service/AuthorService.java`
- change book persistence or author-link rules: `src/main/java/com/bookstore/service/BookService.java`
- change payload shape: DTOs in `controller/` and models in `model/`
- change runtime docs or error handling: `src/main/java/com/bookstore/api/`
- change categories: `src/main/java/com/bookstore/model/Category.java` and `mongo-init/01-init-bookstore.js`

## Related documents

- API contract: `../openapi/openapi.yaml`
- OpenAPI notes: `../openapi/README.md`
- Functional view: `../functional/README.md`
- Repository entrypoint: `../../README.md`
