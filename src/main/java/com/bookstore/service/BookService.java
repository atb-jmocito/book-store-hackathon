package com.bookstore.service;

import com.bookstore.api.ApiException;
import com.bookstore.controller.CreateBookDTO;
import com.bookstore.controller.UpdateBookDTO;
import com.bookstore.model.Book;
import com.bookstore.model.Category;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class BookService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookService.class);

    private final MongoCollection<Document> collection;
    private final AuthorService authorService;

    public BookService(MongoClient mongoClient, String databaseName, String collectionName, AuthorService authorService) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
        this.authorService = authorService;
        this.collection.createIndex(Indexes.ascending("authorId"));
    }

    public List<Book> listBooks(String categoryName, String authorId) {
        List<Bson> filters = new ArrayList<>();
        if (hasText(categoryName)) {
            Category category = resolveCategory(categoryName);
            filters.add(Filters.eq("categoryId", category.getId()));
        }
        if (hasText(authorId)) {
            filters.add(Filters.eq("authorId", AuthorService.toObjectId(authorId, "authorId").toHexString()));
        }
        if (filters.isEmpty()) {
            return mapBooks(collection.find());
        }
        if (filters.size() == 1) {
            return mapBooks(collection.find(filters.getFirst()));
        }
        return mapBooks(collection.find(Filters.and(filters)));
    }

    public Book getBookById(String id) {
        return findById(id).orElseThrow(() -> ApiException.notFound("Book with id '%s' was not found.".formatted(id)));
    }

    public Book getSingleBook(String id, String name, String author) {
        if (hasText(id)) {
            return getBookById(id);
        }
        if (hasText(name)) {
            return findByTitle(name).orElseThrow(() -> ApiException.notFound("Book with name '%s' was not found.".formatted(name)));
        }
        if (hasText(author)) {
            return findByAuthor(author).orElseThrow(() -> ApiException.notFound("Book with author '%s' was not found.".formatted(author)));
        }
        throw ApiException.badRequest("At least one lookup parameter must be supplied: id, name, or author.");
    }

    public Book createBook(CreateBookDTO request) {
        if (request == null) {
            throw ApiException.badRequest("Request body is required.");
        }
        if (!hasText(request.getTitle())) {
            throw ApiException.badRequest("Field 'title' is required.");
        }

        Category category = request.resolveCategoryName() == null ? null : resolveCategory(request.resolveCategoryName());
        AuthorService.AuthorLink authorLink = authorService.resolveAuthorLink(request.getAuthorId(), request.getAuthor());
        ObjectId objectId = new ObjectId();
        Date inactiveDate = request.isActive() ? request.getInactiveDate() : defaultInactiveDate(request.getInactiveDate());

        Document document = new Document("_id", objectId)
                .append("title", request.getTitle().trim())
                .append("authorId", authorLink == null ? null : authorLink.id())
                .append("author", authorLink == null ? trimToNull(request.getAuthor()) : authorLink.name())
                .append("categoryId", category == null ? null : category.getId())
                .append("quantity", request.getQuantity() == null ? 0 : request.getQuantity())
                .append("description", trimToNull(request.getDescription()))
                .append("language", trimToNull(request.getLanguage()))
                .append("active", request.isActive())
                .append("inactiveDate", inactiveDate)
                .append("publisher", trimToNull(request.getPublisher()))
                .append("publisherDate", request.getPublisherDate());

        collection.insertOne(document);
        LOGGER.info("created book id={}", objectId.toHexString());
        return mapBook(document);
    }

    public Book updateBook(String id, UpdateBookDTO request) {
        if (request == null) {
            throw ApiException.badRequest("Request body is required.");
        }
        if (request.getQuantity() == null && request.isActive() == null) {
            throw ApiException.badRequest("At least one mutable field is required: quantity or active.");
        }

        ObjectId objectId = toObjectId(id);
        Document existing = collection.find(Filters.eq("_id", objectId)).first();
        if (existing == null) {
            throw ApiException.notFound("Book with id '%s' was not found.".formatted(id));
        }

        List<org.bson.conversions.Bson> updates = new ArrayList<>();
        if (request.getQuantity() != null) {
            updates.add(Updates.set("quantity", request.getQuantity()));
        }
        if (request.isActive() != null) {
            updates.add(Updates.set("active", request.isActive()));
            updates.add(Updates.set("inactiveDate", Boolean.FALSE.equals(request.isActive()) ? new Date() : null));
        }

        collection.updateOne(Filters.eq("_id", objectId), Updates.combine(updates));

        Document updated = collection.find(Filters.eq("_id", objectId)).first();
        if (updated == null) {
            throw ApiException.notFound("Book with id '%s' was not found after update.".formatted(id));
        }

        LOGGER.info("updated book id={}", id);
        return mapBook(updated);
    }

    private Optional<Book> findById(String id) {
        return Optional.ofNullable(collection.find(Filters.eq("_id", toObjectId(id))).first())
                .map(BookService::mapBook);
    }

    private Optional<Book> findByTitle(String name) {
        return Optional.ofNullable(collection.find(Filters.eq("title", name)).first())
                .map(BookService::mapBook);
    }

    private Optional<Book> findByAuthor(String author) {
        return Optional.ofNullable(collection.find(Filters.eq("author", author)).first())
                .map(BookService::mapBook);
    }

    private static List<Book> mapBooks(Iterable<Document> documents) {
        List<Book> books = new ArrayList<>();
        for (Document document : documents) {
            books.add(mapBook(document));
        }
        return books;
    }

    private static Book mapBook(Document doc) {
        Book book = new Book();
        book.setId(doc.getObjectId("_id").toHexString());
        book.setTitle(doc.getString("title"));
        book.setAuthorId(doc.getString("authorId"));
        book.setAuthor(doc.getString("author"));
        Integer quantity = doc.getInteger("quantity");
        book.setQuantity(quantity == null ? 0 : quantity);
        Integer categoryId = doc.getInteger("categoryId");
        book.setCategoryId(categoryId == null ? 0 : categoryId);
        book.setDescription(doc.getString("description"));
        book.setLanguage(doc.getString("language"));
        book.setActive(doc.getBoolean("active", true));
        book.setInactiveDate(doc.getDate("inactiveDate"));
        book.setPublisher(doc.getString("publisher"));
        book.setPublisherDate(doc.getDate("publisherDate"));
        return book;
    }

    private static Category resolveCategory(String categoryName) {
        return Category.findByName(categoryName)
                .orElseThrow(() -> ApiException.badRequest("Category '%s' is not supported.".formatted(categoryName)));
    }

    private static Date defaultInactiveDate(Date inactiveDate) {
        return inactiveDate == null ? new Date() : inactiveDate;
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static ObjectId toObjectId(String id) {
        return AuthorService.toObjectId(id, "id");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
