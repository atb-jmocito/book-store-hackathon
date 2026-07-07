# FR2 Implementation Tasks

Status values:

- `done` - completed in repository
- `in_progress` - currently being implemented
- `pending` - not started yet

| Order | Status | Task | Notes |
| --- | --- | --- | --- |
| 1 | done | Write feature specification | `spec.md` defines domain model, API changes, migration path, and validation rules. |
| 2 | done | Implement author domain model and endpoints | Added `Author`, `CreateAuthorDTO`, `AuthorController`, and `AuthorService` with create/list/get/soft-delete behavior plus case-insensitive uniqueness. |
| 3 | done | Extend book model and create flow for author linkage | Added additive `authorId`, canonical author resolution, minimal-author auto-create for legacy writes, and compatibility-preserving `author` responses. |
| 4 | done | Add author-based book filtering | `GET /books` now supports `authorId` filtering alone or combined with category. |
| 5 | done | Update runtime contract and bootstrap wiring | App bootstrap now registers author resources, config includes author collection name, OpenAPI updated, and Mongo seed/index setup refreshed. |
| 6 | done | Expand automated coverage | Added author controller/service tests and extended integration/API/book tests for author lifecycle and book linkage. |
| 7 | done | Refresh shared documentation | Updated feature docs, OpenAPI notes, functional docs, technical docs, and root README. |
| 8 | done | Complete QA checklist | `checklist.md` now reflects completed API, persistence, test, and documentation coverage. |

## Progress notes

- Implementation complete.
- Final documentation set under `docs/` reflects shipped FR2 behavior.
