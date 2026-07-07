package com.bookstore.integration;

import com.bookstore.api.ApiExceptionMapper;
import com.bookstore.api.DocumentationResource;
import com.bookstore.api.HealthResource;
import com.bookstore.api.UnhandledExceptionMapper;
import com.bookstore.config.AppConfig;
import com.bookstore.config.RequestTracingFilter;
import com.bookstore.controller.BookController;
import com.bookstore.controller.CreateBookDTO;
import com.bookstore.controller.UpdateBookDTO;
import com.bookstore.model.Book;
import com.bookstore.service.BookService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jsonb.JsonBindingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class BookstoreApiIntegrationTest {
    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    private HttpServer server;
    private Client client;
    private MongoClient mongoClient;
    private MongoCollection<Document> booksCollection;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        int port = randomPort();
        baseUrl = "http://127.0.0.1:%d/api".formatted(port);

        mongoClient = MongoClients.create(MONGO.getReplicaSetUrl());
        MongoDatabase database = mongoClient.getDatabase("bookstore-integration");
        booksCollection = database.getCollection("books");
        booksCollection.deleteMany(new Document());
        booksCollection.insertOne(sampleBookDocument());

        AppConfig appConfig = new AppConfig("127.0.0.1", port, "/api", MONGO.getReplicaSetUrl(), "bookstore-integration", "books", "test", true);
        BookService bookService = new BookService(mongoClient, appConfig.databaseName(), appConfig.collectionName());
        ResourceConfig resourceConfig = new ResourceConfig()
                .register(JsonBindingFeature.class)
                .register(new RequestTracingFilter())
                .register(new ApiExceptionMapper())
                .register(new UnhandledExceptionMapper())
                .register(new HealthResource())
                .register(new DocumentationResource(appConfig))
                .register(new BookController(bookService));

        server = GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUrl + "/"), resourceConfig);
        client = ClientBuilder.newClient();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.shutdownNow();
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Test
    void createBookAcceptsLegacyCategoryAlias() {
        CreateBookDTO request = new CreateBookDTO();
        request.setTitle("Domain-Driven Design");
        request.setAuthor("Eric Evans");
        request.setCategoryId("Programming");
        request.setQuantity(5);

        try (Response response = client.target(baseUrl + "/books")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE))) {
            Book created = response.readEntity(Book.class);

            assertEquals(201, response.getStatus());
            assertEquals(6, created.getCategoryId());
            assertNotNull(response.getHeaderString(RequestTracingFilter.TRACE_ID_HEADER));
        }
    }

    @Test
    void getBookByIdRejectsInvalidObjectIdWithProblemDetails() {
        try (Response response = client.target(baseUrl + "/books/not-an-object-id")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get()) {
            String problem = response.readEntity(String.class);

            assertEquals(400, response.getStatus());
            assertEquals("application/problem+json", response.getMediaType().toString());
            assertTrue(problem.contains("\"title\":\"Bad Request\""));
            assertTrue(problem.contains("urn:trace:"));
        }
    }

    @Test
    void updateByPathReturnsFullEntity() {
        String bookId = booksCollection.find().first().getObjectId("_id").toHexString();
        UpdateBookDTO request = new UpdateBookDTO();
        request.setQuantity(9);
        request.setActive(false);

        try (Response response = client.target(baseUrl + "/books/" + bookId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE))) {
            Book updated = response.readEntity(Book.class);

            assertEquals(200, response.getStatus());
            assertEquals("Clean Code", updated.getTitle());
            assertEquals(9, updated.getQuantity());
            assertEquals(false, updated.isActive());
            assertNotNull(updated.getInactiveDate());
        }
    }

    @Test
    void legacySingleEndpointStillWorks() {
        String bookId = booksCollection.find().first().getObjectId("_id").toHexString();

        try (Response response = client.target(baseUrl + "/books/single")
                .queryParam("id", bookId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get()) {
            Book book = response.readEntity(Book.class);

            assertEquals(200, response.getStatus());
            assertEquals("true", response.getHeaderString("Deprecation"));
            assertEquals(bookId, book.getId());
        }
    }

    @Test
    void runtimeDocumentationEndpointsAreAvailable() {
        try (Response docs = client.target(baseUrl + "/v3/api-docs").request().get();
             Response swaggerUi = client.target(baseUrl + "/swagger-ui.html").request().get()) {
            String openApi = docs.readEntity(String.class);
            String swaggerHtml = swaggerUi.readEntity(String.class);

            assertEquals(200, docs.getStatus());
            assertTrue(openApi.contains("/books/{id}:"));
            assertEquals(200, swaggerUi.getStatus());
            assertTrue(swaggerHtml.contains("SwaggerUIBundle"));
        }
    }

    private static int randomPort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    private static Document sampleBookDocument() {
        return new Document("_id", new ObjectId())
                .append("title", "Clean Code")
                .append("author", "Robert C. Martin")
                .append("categoryId", 6)
                .append("quantity", 3)
                .append("description", "Classic software craftsmanship book")
                .append("language", "English")
                .append("active", true)
                .append("inactiveDate", null)
                .append("publisher", "Prentice Hall")
                .append("publisherDate", null);
    }
}
