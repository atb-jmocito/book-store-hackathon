package com.bookstore.service;

import com.bookstore.api.ApiException;
import com.bookstore.controller.CreateAuthorDTO;
import com.bookstore.model.Author;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AuthorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorService.class);

    private final MongoCollection<Document> collection;

    public AuthorService(MongoClient mongoClient, String databaseName, String collectionName) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
        this.collection.createIndex(Indexes.ascending("nameKey"), new IndexOptions().unique(true));
    }

    public List<Author> listAuthors(Boolean active) {
        if (active == null) {
            return mapAuthors(collection.find());
        }
        return mapAuthors(collection.find(Filters.eq("active", active)));
    }

    public Author getAuthorById(String id) {
        return findById(id)
                .orElseThrow(() -> ApiException.notFound("Author with id '%s' was not found.".formatted(id)));
    }

    public Author createAuthor(CreateAuthorDTO request) {
        if (request == null) {
            throw ApiException.badRequest("Request body is required.");
        }

        String name = validateName(request.getName(), "Field 'name' is required.");
        String nameKey = normalizeName(name);
        String nationality = trimToNull(request.getNationality());
        String email = validateEmail(request.getEmail());

        if (findDocumentByNameKey(nameKey).isPresent()) {
            throw ApiException.conflict("Author with name '%s' already exists.".formatted(name));
        }

        Date now = new Date();
        ObjectId objectId = new ObjectId();
        Document document = new Document("_id", objectId)
                .append("name", name)
                .append("nameKey", nameKey)
                .append("birthDate", request.getBirthDate())
                .append("nationality", nationality)
                .append("email", email)
                .append("active", true)
                .append("createdAt", now)
                .append("updatedAt", now);

        try {
            collection.insertOne(document);
        } catch (MongoWriteException exception) {
            throw ApiException.conflict("Author with name '%s' already exists.".formatted(name));
        }

        LOGGER.info("created author id={}", objectId.toHexString());
        return mapAuthor(document);
    }

    public Author softDeleteAuthor(String id) {
        ObjectId objectId = toObjectId(id, "id");
        Document existing = collection.find(Filters.eq("_id", objectId)).first();
        if (existing == null) {
            throw ApiException.notFound("Author with id '%s' was not found.".formatted(id));
        }
        if (!existing.getBoolean("active", true)) {
            return mapAuthor(existing);
        }

        collection.updateOne(
                Filters.eq("_id", objectId),
                Updates.combine(
                        Updates.set("active", false),
                        Updates.set("updatedAt", new Date())));

        Document updated = collection.find(Filters.eq("_id", objectId)).first();
        if (updated == null) {
            throw ApiException.notFound("Author with id '%s' was not found after delete.".formatted(id));
        }

        LOGGER.info("soft-deleted author id={}", id);
        return mapAuthor(updated);
    }

    public AuthorLink resolveAuthorLink(String authorId, String authorName) {
        if (hasText(authorId)) {
            Author author = findActiveById(authorId)
                    .orElseThrow(() -> ApiException.notFound("Active author with id '%s' was not found.".formatted(authorId)));
            return new AuthorLink(author.getId(), author.getName());
        }
        if (!hasText(authorName)) {
            return null;
        }

        String normalizedName = validateName(authorName, "Field 'author' must not be blank.");
        Optional<Document> existing = findDocumentByNameKey(normalizeName(normalizedName));
        if (existing.isPresent()) {
            Author author = mapAuthor(existing.get());
            if (!author.isActive()) {
                throw ApiException.conflict("Author '%s' is inactive and cannot be linked to a new book.".formatted(author.getName()));
            }
            return new AuthorLink(author.getId(), author.getName());
        }

        CreateAuthorDTO request = new CreateAuthorDTO();
        request.setName(normalizedName);
        Author created = createAuthor(request);
        return new AuthorLink(created.getId(), created.getName());
    }

    public static ObjectId toObjectId(String id, String fieldName) {
        if (!hasText(id)) {
            throw ApiException.badRequest("Field '%s' is required.".formatted(fieldName));
        }

        try {
            return new ObjectId(id.trim());
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("Field '%s' must be a valid MongoDB ObjectId.".formatted(fieldName));
        }
    }

    private Optional<Author> findById(String id) {
        return Optional.ofNullable(collection.find(Filters.eq("_id", toObjectId(id, "id"))).first())
                .map(AuthorService::mapAuthor);
    }

    private Optional<Author> findActiveById(String id) {
        return Optional.ofNullable(collection.find(Filters.and(
                        Filters.eq("_id", toObjectId(id, "authorId")),
                        Filters.eq("active", true)))
                .first())
                .map(AuthorService::mapAuthor);
    }

    private Optional<Document> findDocumentByNameKey(String nameKey) {
        return Optional.ofNullable(collection.find(Filters.eq("nameKey", nameKey)).first());
    }

    private static List<Author> mapAuthors(Iterable<Document> documents) {
        List<Author> authors = new ArrayList<>();
        for (Document document : documents) {
            authors.add(mapAuthor(document));
        }
        return authors;
    }

    private static Author mapAuthor(Document document) {
        Author author = new Author();
        author.setId(document.getObjectId("_id").toHexString());
        author.setName(document.getString("name"));
        author.setBirthDate(document.getDate("birthDate"));
        author.setNationality(document.getString("nationality"));
        author.setEmail(document.getString("email"));
        author.setActive(document.getBoolean("active", true));
        author.setCreatedAt(document.getDate("createdAt"));
        author.setUpdatedAt(document.getDate("updatedAt"));
        return author;
    }

    private static String validateName(String value, String missingMessage) {
        String name = trimToNull(value);
        if (name == null) {
            throw ApiException.badRequest(missingMessage);
        }
        if (name.length() > 255) {
            throw ApiException.badRequest("Field 'name' must be at most 255 characters.");
        }
        return name;
    }

    private static String validateEmail(String value) {
        String email = trimToNull(value);
        if (email == null) {
            return null;
        }
        if (email.chars().filter(character -> character == '@').count() != 1
                || email.startsWith("@")
                || email.endsWith("@")
                || email.chars().anyMatch(Character::isWhitespace)) {
            throw ApiException.badRequest("Field 'email' must be a valid email address.");
        }
        return email;
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record AuthorLink(String id, String name) {
    }
}
