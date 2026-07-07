package com.bookstore.service;

import com.bookstore.api.ApiException;
import com.bookstore.controller.CreateAuthorDTO;
import com.bookstore.model.Author;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorServiceTest {
    private MongoCollection<Document> collection;
    private AuthorService authorService;

    @BeforeEach
    void setUp() {
        MongoClient mongoClient = mock(MongoClient.class);
        MongoDatabase database = mock(MongoDatabase.class);
        collection = mock(MongoCollection.class);

        when(mongoClient.getDatabase("bookstore")).thenReturn(database);
        when(database.getCollection("authors")).thenReturn(collection);

        authorService = new AuthorService(mongoClient, "bookstore", "authors");
    }

    @Test
    void createAuthorPersistsAuthor() {
        FindIterable<Document> findIterable = mock(FindIterable.class);
        when(findIterable.first()).thenReturn(null);
        when(collection.find(any(Bson.class))).thenReturn(findIterable);

        CreateAuthorDTO request = new CreateAuthorDTO();
        request.setName("Robert C. Martin");
        request.setEmail("unclebob@example.com");

        Author created = authorService.createAuthor(request);

        assertEquals("Robert C. Martin", created.getName());
        assertEquals("unclebob@example.com", created.getEmail());
        assertEquals(true, created.isActive());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
        verify(collection).insertOne(any(Document.class));
    }

    @Test
    void createAuthorRejectsDuplicateName() {
        FindIterable<Document> findIterable = mock(FindIterable.class);
        when(findIterable.first()).thenReturn(authorDocument(new ObjectId(), "Robert C. Martin", true));
        when(collection.find(any(Bson.class))).thenReturn(findIterable);

        CreateAuthorDTO request = new CreateAuthorDTO();
        request.setName("Robert C. Martin");

        ApiException exception = assertThrows(ApiException.class, () -> authorService.createAuthor(request));

        assertEquals(409, exception.status().getStatusCode());
    }

    @Test
    void listAuthorsFiltersByActive() {
        FindIterable<Document> findIterable = iterableWith(authorDocument(new ObjectId(), "Robert C. Martin", true));
        when(collection.find(any(Bson.class))).thenReturn(findIterable);

        List<Author> authors = authorService.listAuthors(true);

        assertEquals(1, authors.size());
        assertEquals("Robert C. Martin", authors.getFirst().getName());
    }

    @Test
    void softDeleteAuthorMarksAuthorInactive() {
        ObjectId authorId = new ObjectId();
        Document existing = authorDocument(authorId, "Robert C. Martin", true);
        Document updated = authorDocument(authorId, "Robert C. Martin", false);
        FindIterable<Document> existingFind = mock(FindIterable.class);
        FindIterable<Document> updatedFind = mock(FindIterable.class);
        when(existingFind.first()).thenReturn(existing);
        when(updatedFind.first()).thenReturn(updated);
        when(collection.find(any(Bson.class))).thenReturn(existingFind, updatedFind);

        Author deleted = authorService.softDeleteAuthor(authorId.toHexString());

        assertEquals(false, deleted.isActive());
        assertNotNull(deleted.getUpdatedAt());
        verify(collection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    void resolveAuthorLinkCreatesMinimalAuthorForLegacyBookCreate() {
        FindIterable<Document> emptyFind = mock(FindIterable.class);
        when(emptyFind.first()).thenReturn(null);
        when(collection.find(any(Bson.class))).thenReturn(emptyFind, emptyFind);

        AuthorService.AuthorLink authorLink = authorService.resolveAuthorLink(null, "Robert C. Martin");

        assertNotNull(authorLink);
        assertEquals("Robert C. Martin", authorLink.name());
        verify(collection).insertOne(any(Document.class));
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

    private static Document authorDocument(ObjectId objectId, String name, boolean active) {
        Date now = new Date();
        return new Document("_id", objectId)
                .append("name", name)
                .append("nameKey", name.toLowerCase())
                .append("birthDate", null)
                .append("nationality", "American")
                .append("email", "unclebob@example.com")
                .append("active", active)
                .append("createdAt", now)
                .append("updatedAt", now);
    }
}
