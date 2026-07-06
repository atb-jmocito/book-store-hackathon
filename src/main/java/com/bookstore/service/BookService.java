package com.bookstore.service;

import com.bookstore.model.Book;
import com.bookstore.model.Category;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;

public class BookService {
    private final String mongoUri = "mongodb://localhost:27017";
    private final String databaseName = "bookstore";
    private final String collectionName = "books";

    public BookService() {
    }

    public ArrayList<Book> listBooks() {
        MongoClient client = MongoClients.create(mongoUri);
        MongoDatabase db = client.getDatabase(databaseName);
        MongoCollection<Document> coll = db.getCollection(collectionName);

        ArrayList<Book> books = new ArrayList<>();
        for (Document doc : coll.find()) {
            Book b = new Book();
            b.setId(doc.getObjectId("_id").toHexString());
            b.setTitle(doc.getString("title"));
            b.setAuthor(doc.getString("author"));
            b.setCategoryId(doc.getString("categoryId"));
            b.setDescription(doc.getString("description"));
            b.setDiscontinued(doc.getBoolean("isDiscontinued", false));
            books.add(b);
        }

        return books;
    }

    public Book addBook(String title, String author, String categoryId, String description) {
        try {
            Book b = new Book();
            b.setId(new ObjectId().toHexString());
            b.setTitle(title);
            b.setAuthor(author);
            b.setCategoryId(categoryId);
            b.setDescription(description);

            Document doc = new Document("_id", new ObjectId(b.getId()))
                    .append("title", b.getTitle())
                    .append("author", b.getAuthor())
                    .append("categoryId", b.getCategoryId())
                    .append("description", b.getDescription())
                    .append("isDiscontinued", b.isDiscontinued());

            MongoClient client = MongoClients.create(mongoUri);
            MongoDatabase db = client.getDatabase(databaseName);
            MongoCollection<Document> coll = db.getCollection(collectionName);
            coll.insertOne(doc);
            client.close();

            return b;
        } catch (Exception e) {
            // poor error handling: rethrow without context
            throw new RuntimeException(e);
        }
    }

    public Category validateCategory(String categoryId) {
        // duplicate hardcoded list lookup (intentional smell)
        for (Category c : Category.STATIC_CATEGORIES) {
            if (c.getId().equals(categoryId)) {
                return c;
            }
        }
        // returns null instead of throwing
        return null;
    }
}
