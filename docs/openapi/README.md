# OpenAPI docs

`openapi.yaml` is a hand-authored OpenAPI 3.0.4 snapshot of the current Bookstore runtime.

## Source mapping

- App bootstrap and base path: `src/main/java/com/bookstore/BookstoreApp.java`
- HTTP resource: `src/main/java/com/bookstore/controller/BookController.java`
- Request DTOs: `src/main/java/com/bookstore/controller/CreateBookDTO.java`, `src/main/java/com/bookstore/controller/UpdateBookDTO.java`
- Response/domain model: `src/main/java/com/bookstore/model/Book.java`
- Service behavior and edge cases: `src/main/java/com/bookstore/service/BookService.java`
- Static categories: `src/main/java/com/bookstore/model/Category.java`, `mongo-init/01-init-bookstore.js`

## Intent

This spec mirrors current runtime behavior exactly. It does **not** normalize the API to company-preferred behavior.

## Known gaps vs preferred API standards

- No runtime OpenAPI endpoint or Swagger UI is exposed yet.
- Error responses are not RFC 7807 `application/problem+json`; most documented error cases have no stable body.
- `POST /books` returns `404` when `title` is missing instead of a validation-oriented `400`.
- `PUT /books` returns `404` when `id` is missing instead of a validation-oriented `400`.
- `GET /books/single` returns `200` with `null` when nothing matches instead of `404`.
- `GET /books/single?author=...` throws an unhandled server error because author filtering is stubbed but not implemented.
- Create requests use a field named `categoryId`, but the service expects a category **name** string and converts it to an internal numeric id.
- Update responses return a sparse `Book` object with many null/default fields because the service only mutates quantity, active flag, and inactive date.
- Id lookup in `BookService.getBook(...)` uses Java string reference comparison, so documented id lookups may still return `null` unexpectedly at runtime.

## Next implementation step if runtime docs are needed

Add OpenAPI generation and publishing to the Jersey app so the spec is available from a standard endpoint such as `/v3/api-docs`, then serve a UI in non-production environments.
