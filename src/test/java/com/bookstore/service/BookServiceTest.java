package com.bookstore.service;

import com.bookstore.api.ApiException;
import com.bookstore.controller.CreateBookDTO;
import com.bookstore.controller.UpdateBookDTO;
import com.bookstore.model.Book;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookServiceTest {
    private MongoCollection<Document> collection;
    private BookService bookService;
    private AuthorService authorService;

    @BeforeEach
    void setUp() {
        MongoClient mongoClient = mock(MongoClient.class);
        MongoDatabase database = mock(MongoDatabase.class);
        collection = mock(MongoCollection.class);
        authorService = mock(AuthorService.class);

        when(mongoClient.getDatabase("bookstore")).thenReturn(database);
        when(database.getCollection("books")).thenReturn(collection);

        bookService = new BookService(mongoClient, "bookstore", "books", authorService);
    }

    @Test
    void listBooksFiltersByCategory() {
        FindIterable<Document> findIterable = iterableWith(bookDocument(new ObjectId(), "Clean Code", 6));
        when(collection.find(any(Bson.class))).thenReturn(findIterable);

        List<Book> books = bookService.listBooks("Programming", null);

        assertEquals(1, books.size());
        assertEquals("Clean Code", books.getFirst().getTitle());
    }

    @Test
    void createBookRejectsMissingTitle() {
        CreateBookDTO request = new CreateBookDTO();

        ApiException exception = assertThrows(ApiException.class, () -> bookService.createBook(request));

        assertEquals(400, exception.status().getStatusCode());
        verify(collection, never()).insertOne(any(Document.class));
    }

    @Test
    void createBookUsesLegacyCategoryAlias() {
        CreateBookDTO request = new CreateBookDTO();
        request.setTitle("Domain-Driven Design");
        request.setCategoryId("Programming");
        request.setQuantity(4);

        Book created = bookService.createBook(request);

        assertEquals("Domain-Driven Design", created.getTitle());
        assertEquals(6, created.getCategoryId());
        assertEquals(4, created.getQuantity());
        verify(collection).insertOne(any(Document.class));
    }

    @Test
    void createBookLinksResolvedAuthor() {
        CreateBookDTO request = new CreateBookDTO();
        request.setTitle("Domain-Driven Design");
        request.setAuthorId("507f1f77bcf86cd799439099");
        when(authorService.resolveAuthorLink("507f1f77bcf86cd799439099", null))
                .thenReturn(new AuthorService.AuthorLink("507f1f77bcf86cd799439099", "Eric Evans"));

        Book created = bookService.createBook(request);

        assertEquals("507f1f77bcf86cd799439099", created.getAuthorId());
        assertEquals("Eric Evans", created.getAuthor());
        verify(collection).insertOne(any(Document.class));
    }

    @Test
    void getBookByIdRejectsInvalidObjectId() {
        ApiException exception = assertThrows(ApiException.class, () -> bookService.getBookById("bad-id"));

        assertEquals(400, exception.status().getStatusCode());
    }

    @Test
    void updateBookReturnsPersistedDocument() {
        ObjectId objectId = new ObjectId();
        Document existing = bookDocument(objectId, "Clean Code", 6);
        Document updated = bookDocument(objectId, "Clean Code", 6)
                .append("quantity", 7)
                .append("active", false)
                .append("inactiveDate", new Date());

        FindIterable<Document> existingFind = mock(FindIterable.class);
        FindIterable<Document> updatedFind = mock(FindIterable.class);
        when(existingFind.first()).thenReturn(existing);
        when(updatedFind.first()).thenReturn(updated);
        when(collection.find(any(Bson.class))).thenReturn(existingFind, updatedFind);

        UpdateBookDTO request = new UpdateBookDTO();
        request.setQuantity(7);
        request.setActive(false);

        Book result = bookService.updateBook(objectId.toHexString(), request);

        assertEquals("Clean Code", result.getTitle());
        assertEquals(7, result.getQuantity());
        assertEquals(false, result.isActive());
        assertNotNull(result.getInactiveDate());
        verify(collection).updateOne(any(Bson.class), any(Bson.class));
    }

    private static FindIterable<Document> iterableWith(Document... documents) {
        FindIterable<Document> iterable = mock(FindIterable.class);
        MongoCursor<Document> cursor = mock(MongoCursor.class);

        when(iterable.iterator()).thenReturn(cursor);
        if (documents.length == 0) {
            when(cursor.hasNext()).thenReturn(false);
            return iterable;
        }

        Boolean[] hasNextSequence = new Boolean[documents.length + 1];
        for (int index = 0; index < documents.length; index++) {
            hasNextSequence[index] = true;
        }
        hasNextSequence[documents.length] = false;

        when(cursor.hasNext()).thenReturn(hasNextSequence[0], java.util.Arrays.copyOfRange(hasNextSequence, 1, hasNextSequence.length));
        when(cursor.next()).thenReturn(documents[0], java.util.Arrays.copyOfRange(documents, 1, documents.length));
        return iterable;
    }

    private static Document bookDocument(ObjectId objectId, String title, int categoryId) {
        return new Document("_id", objectId)
                .append("title", title)
                .append("author", "Robert C. Martin")
                .append("categoryId", categoryId)
                .append("quantity", 3)
                .append("description", "Classic software craftsmanship book")
                .append("language", "English")
                .append("active", true)
                .append("inactiveDate", null)
                .append("publisher", "Prentice Hall")
                .append("publisherDate", null);
    }
}
