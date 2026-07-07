# FR2 Quality Assurance Checklist

## Feature behavior

- [x] `POST /authors` creates active author records with timestamps.
- [x] `GET /authors` lists authors and supports optional `active` filtering.
- [x] `GET /authors/{id}` returns active or inactive authors by id.
- [x] `DELETE /authors/{id}` performs soft-delete by setting `active=false`.
- [x] `POST /books` accepts canonical `authorId`.
- [x] `POST /books` preserves legacy `author` compatibility by resolving or auto-creating authors.
- [x] `GET /books` supports `authorId` filtering.
- [x] Legacy `GET /books/single` author lookup remains functional through stored `author` display text.

## Validation and error handling

- [x] Invalid author or book ObjectIds return RFC 7807 bad-request responses.
- [x] Duplicate author names return conflict responses.
- [x] Missing required fields still return validation-oriented problem details.
- [x] Inactive author names are not silently reused for new book creation.

## Persistence and wiring

- [x] Author records persist in dedicated `authors` collection.
- [x] Book records persist additive `authorId` while retaining `author`.
- [x] Application wiring registers author controller and author service.
- [x] Mongo setup defines author uniqueness support and book author filter index support.

## Automated coverage

- [x] Unit coverage added for author controller behavior.
- [x] Unit coverage added for author service create/list/delete/resolve behavior.
- [x] Book unit coverage updated for author linkage.
- [x] Integration coverage added for author lifecycle and book filter flows.
- [x] API documentation endpoint coverage updated for author contract visibility.

## Documentation

- [x] `docs/features/fr2/spec.md` matches shipped behavior.
- [x] `docs/features/fr2/tasks.md` reflects final completion status.
- [x] `docs/openapi/openapi.yaml` documents author endpoints and book author linkage.
- [x] `docs/functional/README.md` reflects author management behavior.
- [x] `docs/technical/README.md` reflects runtime architecture changes.
- [x] Root `README.md` reflects new capabilities and configuration surface.
