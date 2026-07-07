# Functional documentation

## Purpose

This document explains the current business behavior of the Bookstore service. It reflects shipped runtime behavior, including author management introduced in FR2.

## Product summary

Bookstore is a small inventory service for managing bookstore catalog records and first-class author profiles.

Today, system supports these runtime capabilities:

| Capability | Business purpose | Current status |
| --- | --- | --- |
| Health check | Confirm service is reachable | Supported |
| List books | View catalog or inventory list | Supported |
| Get one book | Retrieve one record by title or id | Supported with legacy compatibility route |
| Create book | Add a new catalog/inventory record | Supported |
| Update stock/status | Change quantity and active status of an existing book | Supported |
| List authors | View author profiles, including optional active-state filtering | Supported |
| Get one author | Retrieve one author profile by id | Supported |
| Create author | Add an author profile before linking books | Supported |
| Soft-delete author | Mark author inactive without removing linked books | Supported |

Out of scope in current product:

- no book deletion
- no author update endpoint
- no category management
- no bulk import/export
- no pagination or sorting controls
- no authentication, authorization, or approval workflow

## Core business concepts

| Concept | Meaning in current product | Rules |
| --- | --- | --- |
| Book | Main inventory record | One record combines catalog metadata and stock/status data |
| Author | First-class profile linked to books | One author can be linked to many books |
| Category | Fixed classification for a book | Must be one of six predefined categories; categories are not configurable at runtime |
| Quantity | Inventory count stored on the book record | Any integer value is accepted; no runtime validation prevents negative stock |
| Active book | Whether book is considered active in catalog/inventory | Can be changed only through update flow |
| Active author | Whether author profile is active | `DELETE /authors/{id}` sets `active=false` |
| Inactive date | Timestamp linked to book deactivation | Set automatically when a book is updated to inactive |

## Fixed category model

Current categories are hard-coded and seeded in the database:

1. Fiction
2. Non-Fiction
3. Science
4. Biography
5. Children
6. Programming

Business implications:

- catalog can only classify books into these six categories
- there is no business flow to add, rename, or remove categories
- create requests use `categoryName` as preferred field
- deprecated `categoryId` request alias still accepts category name text during transition
- list filtering uses category name

## Author data captured

Each author record contains:

| Field | Business meaning | Current rule |
| --- | --- | --- |
| `id` | Unique technical identifier | Generated on create |
| `name` | Canonical author name | Required, trimmed, max 255 chars, unique ignoring case |
| `birthDate` | Author birth date | Optional |
| `nationality` | Country or nationality text | Optional |
| `email` | Contact email | Optional, basic format validation |
| `active` | Whether author is active | Defaults to `true`; set to `false` on soft-delete |
| `createdAt` | Creation timestamp | Auto-generated |
| `updatedAt` | Last change timestamp | Auto-generated |

## Book data captured

Each book record can contain:

| Field | Business meaning | Current rule |
| --- | --- | --- |
| `id` | Unique technical identifier | Generated on create |
| `title` | Book title | Only field required for create |
| `authorId` | Canonical linked author identifier | Optional but preferred for new writes |
| `author` | Author display name | Preserved for backward compatibility; populated from canonical author when linked |
| `categoryId` | Category classification | Returned as numeric category id; create accepts category name input |
| `quantity` | Stock quantity | Optional on create; defaults to `0` when omitted |
| `description` | Free-text description | Optional |
| `language` | Book language | Optional |
| `active` | Availability/active flag | Defaults to `true` on create |
| `inactiveDate` | Date tied to inactive status | Auto-generated on deactivation update |
| `publisher` | Publisher name | Optional on create |
| `publisherDate` | Publication date | Optional on create |

## Functional flows and business rules

### 1. Health check

Rules:

- health check returns operational JSON payload with `status` and `traceId`
- success means service responds, not that catalog data is complete

### 2. List books

Purpose: retrieve catalog and inventory records.

Rules:

- when no filters are supplied, system returns all books
- when `category` matches one of six predefined category names, system filters by category
- when `authorId` is supplied, system filters by linked author identity
- when both `category` and `authorId` are supplied, both filters are applied
- when category is invalid, request is rejected with validation error details
- when `authorId` format is invalid, request is rejected with validation error details
- valid-but-missing `authorId` returns an empty list
- response is a plain list of books with no pagination

### 3. Get one book

Primary behavior:

- `GET /books/{id}` returns one book by MongoDB ObjectId

Legacy compatibility behavior:

- `GET /books/single` still accepts lookup by `id`, `name`, or `author`
- precedence is `id`, then `name`, then `author`
- legacy `author` lookup still works because linked books retain `author` text

### 4. Create book

Purpose: add a new book record to catalog and inventory.

Rules:

- create succeeds only when `title` is present
- `authorId` is preferred for canonical author linkage
- when `authorId` is present, it must point to an existing active author
- when `authorId` is absent and legacy `author` text is present, system resolves an active author by exact trimmed name ignoring case
- when no active author matches legacy `author` text, system auto-creates a minimal author profile and links the book to it
- when matching author name exists but is inactive, request is rejected with conflict error details
- all other fields remain optional at runtime
- invalid category values are rejected with validation-oriented error details
- quantity defaults to `0` when omitted
- active defaults to `true` when omitted
- system does not prevent duplicate books
- system does not validate inventory rules such as non-negative quantity

### 5. Update stock and active status

Purpose: change inventory quantity and active/inactive state for an existing book.

Rules:

- update succeeds only when `id` is present
- update flow only changes `quantity`, `active`, and `inactiveDate`
- update flow does not change title, author linkage, category, description, language, publisher, or publisher date
- when book is updated with `active = false`, system sets `inactiveDate` to current system time
- when book is updated with `active = true`, system clears `inactiveDate`
- negative quantities are still accepted

### 6. List authors

Purpose: retrieve author profiles.

Rules:

- when no filter is supplied, system returns both active and inactive authors
- `active=true` returns only active authors
- `active=false` returns only inactive authors
- response is a plain list with no pagination

### 7. Get one author

Purpose: retrieve a single author profile.

Rules:

- author lookup uses MongoDB ObjectId
- inactive authors remain retrievable
- malformed ids are rejected as bad requests
- missing ids return not-found problem details

### 8. Create author

Purpose: create an author profile before linking books.

Rules:

- create succeeds only when `name` is present
- names are unique ignoring case
- duplicate names return conflict problem details
- optional email is validated with a basic address format rule
- record is created active by default

### 9. Soft-delete author

Purpose: deactivate an author without removing history or linked books.

Rules:

- soft-delete sets `active=false`
- linked books remain queryable and keep author display text
- repeated soft-delete returns the inactive author representation
- soft-delete does not cascade into book deactivation

## Lifecycle rules

### Book lifecycle

1. Book is created with metadata, quantity, active flag, and optional canonical author link.
2. Book remains queryable whether active or inactive.
3. Quantity and active status can be changed later.
4. There is still no book delete or archive flow.

### Author lifecycle

1. Author is created explicitly through `/authors` or implicitly during legacy book creation.
2. Author can be linked to many books.
3. Author can be soft-deleted later.
4. Soft-deleted author remains readable by id and list filters.

## Cross-cutting business limitations

- no user roles or access control
- no audit trail of who changed a book or author
- no approval step before a book or author becomes inactive
- no author update flow for correcting profile details after creation
- no bulk operations

## Source traceability

This document was derived from current implementation and supporting docs:

- API contract: `../openapi/openapi.yaml`
- OpenAPI notes: `../openapi/README.md`
- Technical overview: `../technical/README.md`
- Runtime source:
  - `src/main/java/com/bookstore/controller/AuthorController.java`
  - `src/main/java/com/bookstore/controller/BookController.java`
  - `src/main/java/com/bookstore/service/AuthorService.java`
  - `src/main/java/com/bookstore/service/BookService.java`
  - `src/main/java/com/bookstore/controller/CreateAuthorDTO.java`
  - `src/main/java/com/bookstore/controller/CreateBookDTO.java`
  - `src/main/java/com/bookstore/controller/UpdateBookDTO.java`
  - `src/main/java/com/bookstore/model/Author.java`
  - `src/main/java/com/bookstore/model/Book.java`
  - `src/main/java/com/bookstore/model/Category.java`
