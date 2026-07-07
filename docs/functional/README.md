# Functional documentation

## Purpose

This document explains current Bookstore behavior for product owners and business managers. It describes what business capabilities exist today, which rules govern them, and which runtime quirks materially affect users or downstream consumers.

This is a **current-state** document. It reflects runtime behavior as implemented, even when that behavior is surprising or imperfect.

## Product summary

Bookstore is a small inventory service for managing a bookstore catalog of books.

Today, system supports only five runtime capabilities:

| Capability | Business purpose | Current status |
| --- | --- | --- |
| Health check | Confirm service is reachable | Supported |
| List books | View catalog or inventory list | Supported |
| Get one book | Retrieve one record by title or id | Supported with limitations |
| Create book | Add a new catalog/inventory record | Supported |
| Update stock/status | Change quantity and active status of an existing record | Supported |

Out of scope in current product:

- no book deletion
- no category management
- no author search
- no bulk import/export
- no pagination or sorting controls
- no authentication, authorization, or approval workflow

## Core business concepts

| Concept | Meaning in current product | Rules |
| --- | --- | --- |
| Book | Main inventory record | One record combines catalog metadata and stock/status data |
| Category | Fixed classification for a book | Must be one of six predefined categories; categories are not configurable at runtime |
| Quantity | Inventory count stored on the book record | Any integer value is accepted; no runtime validation prevents negative stock |
| Active | Whether book is considered active in catalog/inventory | Can be changed only through update flow |
| Inactive date | Timestamp linked to deactivation | Set automatically when a book is updated to inactive |
| Publisher date | Publication date supplied with the book | Stored as provided on create; not editable through update flow |

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
- create requests use a field named `categoryId`, but runtime expects the **category name** (for example `Programming`), not numeric id `6`
- list filtering also uses category name

## Book data captured

Each book record can contain:

| Field | Business meaning | Current rule |
| --- | --- | --- |
| `id` | Unique technical identifier | Generated on create unless explicitly supplied internally |
| `title` | Book title | Only field required for create |
| `author` | Author name | Optional on create; not searchable in supported runtime flow |
| `categoryId` | Category classification | Returned and stored as numeric category id, but supplied as category name during create |
| `quantity` | Stock quantity | Optional on create; defaults to `0` when omitted |
| `description` | Free-text description | Optional |
| `language` | Book language | Optional |
| `active` | Availability/active flag | Defaults to `true` on create |
| `inactiveDate` | Date tied to inactive status | Auto-generated on deactivation update |
| `publisher` | Publisher name | Optional on create |
| `publisherDate` | Publication date | Optional on create |

## Functional flows and business rules

### 1. Health check

Purpose: operational confirmation that API is up.

Rules:

- health check has no business payload
- success means service responds, not that catalog data is valid or complete

### 2. List books

Purpose: retrieve catalog/inventory records.

Rules:

- when no category is supplied, system returns all books
- when category matches one of six predefined category names, system returns only books in that category
- when category does **not** match a predefined category, system rejects request with validation error details
- response is a plain list of books with no pagination
- no sort order is defined as business behavior

### 3. Get one book

Purpose: retrieve a single book record.

Current lookup behavior:

- system accepts lookup by `name`, `id`, or `author`
- title lookup uses exact title equality
- if more than one lookup parameter is supplied, `id` has precedence, then `name`, then `author`

Business-relevant quirks:

- when nothing matches, system returns not-found problem details
- id lookup validates MongoDB ObjectId format and rejects malformed ids as bad requests

### 4. Create book

Purpose: add a new book record to catalog/inventory.

Rules:

- create succeeds only when `title` is present
- all other fields are optional at runtime
- `categoryName` is preferred for create requests
- legacy `categoryId` is still accepted during transition, but still carries category **name** text rather than numeric id
- invalid category values are rejected with validation-oriented error details
- quantity defaults to `0` when omitted
- active defaults to `true` when omitted
- system does not prevent duplicate books
- no uniqueness rule exists for title, author, or title-plus-author combinations
- system does not validate business quality of optional fields such as author, publisher, description, or language
- system does not validate inventory rules such as non-negative quantity

Output behavior:

- successful create returns created book payload
- missing `title` is rejected with RFC 7807 validation error details

### 5. Update stock and active status

Purpose: change inventory quantity and active/inactive state for an existing book.

Rules:

- update succeeds only when `id` is present
- update flow only changes:
  - `quantity`
  - `active`
  - `inactiveDate`
- update flow does **not** change title, author, category, description, language, publisher, or publisher date
- when book is updated with `active = false`, system sets `inactiveDate` to current system time
- when book is updated with `active = true`, system clears `inactiveDate`
- `active` is optional in update requests and is only changed when explicitly supplied
- quantity has no business validation, so negative values are accepted

Output behavior:

- successful update returns a full refreshed book payload
- missing `id` is rejected with RFC 7807 validation error details

## Lifecycle rules

Book lifecycle in current product is simple:

1. Book is created with metadata, quantity, and active flag.
2. Book remains queryable whether active or inactive.
3. Quantity and active status can be changed later.
4. Deactivation stamps current inactive date.
5. There is no delete or archive flow.

Implications:

- inactive books remain part of catalog data
- inactive books still appear in list and single-book lookup results; active flag does not automatically hide them
- system behaves more like inventory record maintenance than full catalog lifecycle management

## Cross-cutting business limitations

Current product has several constraints that matter to business stakeholders:

- no user roles or access control
- no audit trail of who changed a book
- no approval step before a book becomes active/inactive
- no validation workflow yet for inventory rules such as non-negative quantity
- no bulk operations
- author lookup is supported on the legacy single-book compatibility route
- no support for editing descriptive metadata after create

## Known behavior quirks to keep in mind

These are not desired-state recommendations. They are current behaviors that affect real usage:

- create field name `categoryId` remains as deprecated compatibility alias and still carries category name text
- legacy compatibility endpoints remain exposed and should be phased out after client migration

## Source traceability

This document was derived from current implementation and supporting docs:

- API contract snapshot: `../openapi/openapi.yaml`
- OpenAPI notes: `../openapi/README.md`
- Technical overview: `../technical/README.md`
- Runtime source:
  - `src/main/java/com/bookstore/controller/BookController.java`
  - `src/main/java/com/bookstore/service/BookService.java`
  - `src/main/java/com/bookstore/controller/CreateBookDTO.java`
  - `src/main/java/com/bookstore/controller/UpdateBookDTO.java`
  - `src/main/java/com/bookstore/model/Category.java`
  - `mongo-init/01-init-bookstore.js`
