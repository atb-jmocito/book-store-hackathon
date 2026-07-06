package com.bookstore.service;

import com.bookstore.model.Book;
import com.bookstore.model.Category;
import com.mongodb.client.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;

import static com.bookstore.model.Category.STATIC_CATEGORIES;

public class BookService {
    private final String mongoUri = "mongodb://mongodb:27017";
    private final String databaseName = "bookstore";
    private final String collectionName = "books";

    public BookService() {
    }


    public ArrayList<Book> getBookList(String category) {
        ArrayList<Book> result = new ArrayList<>();

        if (category != null) {
            int categoryId = 0;
            for(int i = 0; i < STATIC_CATEGORIES.size(); i++) {
                Category c = STATIC_CATEGORIES.get(i);
                if (c.getName().equals(category)) {
                    categoryId = c.getId();
                }
            }

            if(categoryId > 0) {
                ArrayList<Book> listBooks = listBooks();
                for (int i = 0, listBooksSize = listBooks.size(); i < listBooksSize; i++) {
                    Book b = listBooks.get(i);
                    if (b.getCategoryId() == categoryId) {
                        result.add(b);
                    }
                }
            } else {
                result = listBooks();
            }
        } else {
            result = listBooks();
        }

        return result;
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
            Integer quantity = doc.getInteger("quantity");
            b.setQuantity(quantity != null ? quantity : 0);
            Integer categoryId = doc.getInteger("categoryId");
            b.setCategoryId(categoryId != null ? categoryId : 0);
            b.setDescription(doc.getString("description"));
            b.setLanguage(doc.getString("language"));
            b.setActive(doc.getBoolean("active", false));
            b.setInactiveDate(doc.getDate("inactiveDate"));
            b.setPublisher(doc.getString("publisher"));
            b.setPublisherDate(doc.getDate("publisherDate"));
            books.add(b);
        }

        client.close();
        return books;
    }

    public Book upsertBook(String id, String title, String author, String categoryId, int quantity, String description, String language, boolean active, Date inactiveDate, String publisher, Date publisherDate) {
            Book b = new Book();
            Document doc = null;

            String bookId = id != null && !id.isBlank() ? id : new ObjectId().toHexString();
            b.setId(bookId);
            b.setTitle(title);
            b.setAuthor(author);
            if (categoryId != null) {
                b.setCategoryId(validateCategory(categoryId).getId());
            }
            b.setQuantity(quantity);
            b.setDescription(description);
            b.setLanguage(language);
            b.setActive(active);
            b.setInactiveDate(inactiveDate);
            b.setPublisher(publisher);
            b.setPublisherDate(publisherDate);

            // Preparate database
            MongoClient client = MongoClients.create(mongoUri);
            MongoDatabase db = client.getDatabase(databaseName);
            MongoCollection<Document> coll = db.getCollection(collectionName);

            if (id == null) {
                doc = new Document("_id", new ObjectId(b.getId()))
                        .append("title", b.getTitle())
                        .append("author", b.getAuthor())
                        .append("categoryId", b.getCategoryId())
                        .append("quantity", b.getQuantity())
                        .append("description", b.getDescription())
                        .append("language", b.getLanguage())
                        .append("active", b.isActive())
                        .append("inactiveDate", b.getInactiveDate())
                        .append("publisher", b.getPublisher())
                        .append("publisherDate", b.getPublisherDate());


                coll.insertOne(doc);
                client.close();
            } else {
                if(active == false)
                    b.setInactiveDate(new Date());

                doc = new Document("_id", new ObjectId(b.getId()))
                        .append("quantity", b.getQuantity())
                        .append("active", b.isActive())
                        .append("inactiveDate", b.getInactiveDate());

                coll.updateOne(new Document("_id", new ObjectId(b.getId())), new Document("$set", doc));

                client.close();
            }


            return b;
    }

    public Category validateCategory(String category) {
        for (Category c : STATIC_CATEGORIES) {
            if (c.getName().equals(category)) {
                return c;
            }
        }

        return null;
    }

    public Book getBook(String Id, String name) {
        Book result = null;
        if (name != null) {
            ArrayList<Book> listBooks = listBooks();
            for (int i = 0, listBooksSize = listBooks.size(); i < listBooksSize; i++) {
                Book b = listBooks.get(i);
                if (b.getTitle().equals(name)) {
                    result = b;
                    break;
                }
            }
        }

        if (Id != null) {
            ArrayList<Book> listBooks = listBooks();
            for (int i = 0, listBooksSize = listBooks.size(); i < listBooksSize; i++) {
                Book b = listBooks.get(i);
                if (b.getId() == Id) {
                    result = b;
                    break;
                }
            }
        }

        return result;

    }
}
