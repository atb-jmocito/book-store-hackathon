# Feature Request 2 Specification: Author Support

## Status

- Feature id: FR2
- Source issue: `#2` - `FEAT-010: Author Support`
- Delivery mode: documentation-first

## Problem

Book records currently store `author` as plain text. That blocks author lifecycle management, duplicates author names across books, and makes author-centric queries unreliable. System needs first-class author entities plus book-to-author linkage without breaking current book consumers.

## Goals

1. Introduce Authors as first-class managed entities.
2. Support author create, list, get, and soft-delete operations.
3. Link books to authors through stable author identity.
4. Preserve current book API usability during migration from string-only author data.
5. Keep OpenAPI and repository docs aligned with delivered behavior.

## Non-goals

- No author update endpoint in this feature.
- No hard delete for authors.
- No bulk migration job for existing books.
- No authentication or authorization changes.

## Current state

- `Book` API payload exposes only `author` as free text.
- MongoDB stores books in single `books` collection.
- No `authors` collection exists.
- Book list endpoint supports category filtering only.
- Legacy single-book lookup supports exact `author` string lookup.

## Target domain model

### Author

Author becomes a dedicated collection record with lifecycle metadata:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | yes | MongoDB ObjectId exposed as hex string |
| `name` | string | yes | trimmed, non-blank, max 255 chars |
| `birthDate` | date-time | no | stored as Java `Date` |
| `nationality` | string | no | trimmed, nullable |
| `email` | string | no | trimmed, nullable; basic format validation |
| `active` | boolean | yes | defaults to `true`; `false` means soft-deleted |
| `createdAt` | date-time | yes | set on create |
| `updatedAt` | date-time | yes | set on create and every soft-delete/update-internal change |

### Book changes

Books remain the primary inventory entity, but now carry canonical author linkage:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `authorId` | string | no | preferred canonical reference to `authors._id` |
| `author` | string | no | display name kept for backward compatibility |

Rules:

1. `authorId` is authoritative when present.
2. `author` remains in responses so current clients do not break.
3. New book creation supports author resolution via `authorId` first, then legacy `author` text compatibility path.

## Relationship model

- One author can be linked to many books.
- One book can reference at most one author in this feature.
- Books do not embed full author profiles; they expose `authorId` plus `author` name.
- Soft-deleting an author does not remove or hide linked books.

## Persistence design

### Collections

- Existing: `books`
- New: `authors`

### Author document shape

```json
{
  "_id": "ObjectId",
  "name": "Robert C. Martin",
  "birthDate": "Date|null",
  "nationality": "American",
  "email": "unclebob@example.com",
  "active": true,
  "createdAt": "Date",
  "updatedAt": "Date"
}
```

### Book document shape after FR2

```json
{
  "_id": "ObjectId",
  "title": "Clean Code",
  "authorId": "ObjectId|null",
  "author": "Robert C. Martin",
  "categoryId": 6,
  "quantity": 3,
  "description": "Classic software craftsmanship book",
  "language": "English",
  "active": true,
  "inactiveDate": null,
  "publisher": "Prentice Hall",
  "publisherDate": null
}
```

### Indexes

- `authors.name` unique, case-insensitive through normalized application lookup
- `books.authorId` non-unique for filter performance

Application must create missing indexes at startup/service construction so local and test environments stay consistent.

## API design

### Author endpoints

#### `POST /authors`

Creates an author.

Request body:

```json
{
  "name": "Robert C. Martin",
  "birthDate": "1952-12-05T00:00:00Z",
  "nationality": "American",
  "email": "unclebob@example.com"
}
```

Behavior:

- `name` required
- duplicate active author names rejected with `409 Conflict`
- soft-deleted duplicate names also rejected to avoid ambiguous resurrection semantics in this feature
- returns `201 Created` with full author payload

#### `GET /authors`

Lists authors.

Query parameters:

- `active` optional boolean filter

Behavior:

- no filter: return all authors
- `active=true`: only active authors
- `active=false`: only soft-deleted authors

#### `GET /authors/{id}`

Returns one author by MongoDB ObjectId.

Behavior:

- inactive authors remain retrievable
- invalid ObjectId returns `400`
- missing author returns `404`

#### `DELETE /authors/{id}`

Soft-deletes an author.

Behavior:

- sets `active=false`
- updates `updatedAt`
- keeps record retrievable by `GET /authors/{id}`
- repeated delete on already inactive author returns current inactive representation with `200 OK`

### Book endpoint changes

#### `GET /books`

Add optional `authorId` filter.

Behavior:

- supports `category`, `authorId`, or both together
- invalid `authorId` format returns `400`
- valid-but-missing `authorId` returns empty list

#### `POST /books`

Add canonical author resolution.

Request fields:

- `authorId` optional, preferred
- `author` optional, legacy compatibility field

Behavior:

1. If `authorId` present, it must reference an existing author. Response uses referenced author name in `author`.
2. If `authorId` absent and `author` present, service looks up active author by exact trimmed name ignoring case.
3. If no active author matches legacy `author` text, service auto-creates a minimal active author record with only `name` populated, then links the book to it.
4. If neither `authorId` nor `author` is present, book remains authorless.

This preserves compatibility while ensuring newly created books become linked to first-class authors.

#### `GET /books/{id}` and list responses

Book response adds `authorId` and keeps `author`.

#### Legacy `GET /books/single`

Keep current exact-string `author` lookup behavior for compatibility. When books are author-linked, stored `author` field remains populated from canonical author name so legacy behavior still works.

## Validation and error handling

- Keep RFC 7807 responses.
- Reject blank strings after trimming for required fields.
- `Author.name` max 255 characters.
- `Author.email`, when present, must contain exactly one `@`, non-empty local/domain parts, and no whitespace.
- `authorId` path/query/body values must be valid MongoDB ObjectIds.
- Unsupported category behavior remains unchanged.

## Migration and backward compatibility

### Existing books

- Existing records with only `author` string remain readable.
- Existing records are not rewritten automatically.
- Existing records may have `authorId = null`.

### New books

- New create flow should populate both `authorId` and `author` whenever an author is resolved or auto-created.

### API compatibility

- `author` response field remains available.
- `authorId` is additive.
- No existing route is removed in this feature.

## Documentation impact

These documents must reflect delivered behavior:

- `docs/openapi/openapi.yaml`
- `docs/openapi/README.md`
- `docs/functional/README.md`
- `docs/technical/README.md`
- `README.md` if capability summary or setup details change

## Test expectations

Coverage must include:

1. author create success and duplicate rejection
2. author list filter behavior
3. author get by id
4. author soft-delete behavior
5. book create with explicit `authorId`
6. book create with legacy `author` auto-link or auto-create path
7. book list filter by `authorId`
8. legacy author lookup still working for linked books
9. invalid ObjectId handling across author and book filters
