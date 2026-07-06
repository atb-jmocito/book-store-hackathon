# Plan: Legacy Bookstore API - Phase 1: Getting Started Test

## TL;DR
Create a deliberately "legacy MVP" Bookstore REST API (Java 11, Maven, Jakarta EE, MongoDB) as a baseline for future refactoring, documentation, optimization, testing, and evolution. This phase establishes the foundation with intentional code smells and anti-patterns that will be systematically addressed in future phases. Phase 1 includes: project setup, minimal data models, controller endpoint (POST /books), basic service and repository stubs, and a single JUnit 5 + Mockito unit test for the POST endpoint. Code exhibits: hardcoded values, no input validation, tight coupling, poor error handling (unhandled exceptions → 500), and potential NullPointerExceptions.

---

## Steps

### Phase 1.1: Project Setup
1. Create Maven project structure with directory layout:
   - `bookstore-java/` (root)
   - `src/main/java/com/bookstore/` (application code)
   - `src/test/java/com/bookstore/` (unit tests)
   - `src/main/resources/` (config files)
2. Create `pom.xml` with dependencies:
   - **Java 11 compiler**: `maven-compiler-plugin` with source/target 11
   - **Jakarta REST**: `jakarta.ws.rs:jakarta.ws.rs-api:3.1.0`
   - **JAX-RS implementation**: `org.glassfish.jersey:jersey-container-grizzly2-http:3.1.0` (or RESTEasy equivalent)
   - **MongoDB driver**: `org.mongodb:mongodb-driver-sync:4.11.0`
   - **JSON-B**: `jakarta.json.bind:jakarta.json.bind-api:2.0.0` + implementation (e.g., Yasson)
   - **Jakarta DI**: `jakarta.inject:jakarta.inject-api:2.0.0`
   - **Testing**: `org.junit.jupiter:junit-jupiter:5.9.0`, `org.mockito:mockito-core:5.2.0`, `org.mockito:mockito-junit-jupiter:5.2.0`
3. Create basic Maven project structure with packaging set to JAR

### Phase 1.2: Minimal Data Model (No Validation)
4. Create `src/main/java/com/bookstore/model/Book.java`:
   - Fields: `id` (String, MongoDB ObjectId), `title`, `author`, `categoryId` (String), `description`, `isDiscontinued` (boolean)
   - **Code smell**: No `@NotNull`, `@NotBlank` or validation annotations; getters/setters with no guards
   - **Code smell**: Hardcoded default values (e.g., `isDiscontinued = false` in constructor)
   - No equals/hashCode (will cause future issues)

5. Create `src/main/java/com/bookstore/model/Category.java`:
   - Fields: `id` (String), `name` (String)
   - **Code smell**: Hardcoded 6 categories as static list directly in the class (not in config or enum)

### Phase 1.3: MVP Controller (Tightly Coupled)
6. Create `src/main/java/com/bookstore/controller/BookController.java`:
   - Annotate with `@Path("/books")` and `@Produces(MediaType.APPLICATION_JSON)`
   - Implement `POST /books` endpoint (`@POST`, `@Consumes(MediaType.APPLICATION_JSON)`):
     - **Code smell**: Direct instantiation of BookService inside the endpoint (no dependency injection, tight coupling)
     - **Code smell**: No null checks on request body or fields
     - **Code smell**: Returns raw exception as 500 if any operation fails (no try-catch, no global exception handler)
     - **Potential NPE**: Accesses `request.getTitle()` without null check; if null, NullPointerException thrown → 500
     - **Hardcoded string**: Magic string "books" in success response
   - Endpoint accepts JSON: `{ "title": "...", "author": "...", "categoryId": "...", "description": "..." }`
   - Returns 201 Created with created book or 500 on error

### Phase 1.4: MVP Service Layer (No Validation, Poor Error Handling)
7. Create `src/main/java/com/bookstore/service/BookService.java`:
   - **Code smell**: Hardcoded MongoDB connection string (e.g., "mongodb://localhost:27017")
   - **Tight coupling**: Directly instantiates `BookRepository` in the constructor (no DI, no interface)
   - Method `addBook(title, author, categoryId, description)`:
     - **No input validation**: Doesn't check if title is empty, null, or already exists
     - **Poor error handling**: Catches generic `Exception` and rethrows without context
     - Calls `repository.save(book)` expecting it to work
   - Method `validateCategory(categoryId)`:
     - **Code smell**: Hardcoded category list (duplicated from Category class or scattered magic strings)
     - **Potential NPE**: Returns null instead of throwing exception if category not found; caller must null-check

### Phase 1.5: MVP Repository (Coupled to MongoDB)
8. Create `src/main/java/com/bookstore/repository/BookRepository.java` (not an interface; concrete class):
   - **Code smell**: Hardcoded MongoDB URI, database name, collection name as private fields
   - **Tight coupling**: Directly manages MongoClient lifecycle (no singleton pattern, new instance on each instantiation)
   - Method `save(book)`: Inserts directly to MongoDB collection without error handling or null checks
   - **Potential NPE**: `book.getId()` called without null check; if null, throws NPE
   - No connection pooling (creates new connection per call)

### Phase 1.6: Getting-Started Unit Test (JUnit 5 + Mockito)
9. Create `src/test/java/com/bookstore/controller/BookControllerTest.java`:
   - **Test class**: `BookControllerTest`
   - **Test method**: `testCreateBookSuccess()` 
     - **Scope**: Tests POST /books endpoint with valid request
     - **Setup**: Mock `BookService` using Mockito
     - **Arrange**: 
       - Create a test `BookCreateRequest` with valid title, author, categoryId, description
       - Mock `BookService.addBook()` to return a created `Book` object
       - Inject mocked service into controller
     - **Act**: Call controller's `createBook(request)`
     - **Assert**: 
       - Verify response status is 201 Created
       - Verify response body contains the created book
       - Verify `bookService.addBook()` was called once with correct parameters
   - **Code smell**: Test setup is minimal; doesn't test error cases (invalid input, service errors)
   - **Code smell**: Uses concrete `BookService` instead of interface; tight test coupling
   - **Assertion scope**: Only verifies happy path; doesn't cover NPE or validation scenarios

### Phase 1.7: Maven Configuration & Build
10. Configure `pom.xml`:
    - Set Java 11 as target
    - Add `maven-shade-plugin` or `maven-assembly-plugin` for fat JAR (optional for Phase 1)
    - Ensure tests run with `mvn clean test`

### Phase 1.8: Application Startup Stub
11. Create `src/main/java/com/bookstore/BookstoreApp.java`:
    - **Code smell**: Hardcoded port (8080) and base path ("/api")
    - **Hardcoded values**: No config file; all settings inline
    - Entry point creates JAX-RS Application and starts HTTP server
    - No graceful shutdown or resource cleanup

---

## Relevant Files to Create
- `bookstore-java/pom.xml` — Maven configuration (Java 11, Jakarta EE, MongoDB, JUnit 5, Mockito)
- `src/main/java/com/bookstore/BookstoreApp.java` — Application entry point (hardcoded config)
- `src/main/java/com/bookstore/model/Book.java` — Book model (no validation, hardcoded defaults)
- `src/main/java/com/bookstore/model/Category.java` — Category model (hardcoded 6 categories)
- `src/main/java/com/bookstore/controller/BookController.java` — REST endpoint (tightly coupled, no error handling, potential NPE)
- `src/main/java/com/bookstore/service/BookService.java` — Service layer (hardcoded MongoDB URI, poor validation)
- `src/main/java/com/bookstore/repository/BookRepository.java` — Repository (concrete class, hardcoded connection, no pooling)
- `src/test/java/com/bookstore/controller/BookControllerTest.java` — Getting-started unit test (happy path only, Mockito, JUnit 5)

---

## Code Smells & Anti-Patterns (Intentional)
1. **Hardcoded Values**: MongoDB URI, port, base path, category list scattered throughout code
2. **Tight Coupling**: No dependency injection; controller instantiates service; service instantiates repository; repository manages its own MongoDB connection
3. **No Input Validation**: Request fields not validated; null checks absent
4. **Poor Error Handling**: Exceptions propagate as 500 errors; no global exception mapper; no context in error messages
5. **Potential NullPointerExceptions**: 
   - Request body not null-checked in controller
   - Book fields not null-checked in repository save
   - Category lookup returns null instead of exception
6. **No Interfaces**: Repository and Service are concrete classes (complicates testing and refactoring)
7. **Magic Strings**: Hardcoded "books", collection names, response fields
8. **No Configuration Management**: All settings hardcoded in source; no `application.properties` or environment variables
9. **Single Endpoint**: Only POST /books implemented; GET, PATCH stubs for future phases

---

## Verification
1. **Build**: `mvn clean package` compiles successfully (target JAR created)
2. **Unit Test**: `mvn clean test` runs BookControllerTest and passes
3. **Test Coverage**: Test verifies POST /books with mocked service returns 201 and correct response body
4. **Code Smells Present**: Code review confirms hardcoded values, tight coupling, no validation, poor error handling

---

## Decisions & Scope
- **Single test only**: Covers happy path for POST /books; error cases deferred to future phases
- **No validation layer**: Input validation minimal/absent; will be refactored later
- **Concrete classes, not interfaces**: Reflects MVP shortcuts; enables seeing tight coupling clearly for refactoring
- **Hardcoded MongoDB connection**: Simplifies Phase 1; proper config/connection pooling in later phases
- **No global exception handler**: Exceptions bubble up as 500; demonstrates anti-pattern for future improvement
- **JUnit 5 + Mockito only**: No integration tests, no embedded MongoDB (deferred to Phase 2)
- **Categories hardcoded**: Not wired to MongoDB in Phase 1; validation against static list only

---

## Evolution Roadmap (Future Phases)
1. **Phase 2**: Document code smells; add integration tests with embedded MongoDB
2. **Phase 3**: Refactor to use interfaces; introduce proper dependency injection (Jakarta CDI or similar)
3. **Phase 4**: Extract hardcoded values to `application.properties`; add configuration management
4. **Phase 5**: Add input validation; implement global exception handler with proper HTTP status codes
5. **Phase 6**: Optimize MongoDB connection pooling; add database indices
6. **Phase 7**: Implement remaining endpoints (GET, PATCH); add comprehensive test suite
7. **Phase 8**: Performance optimization; add caching, async operations
