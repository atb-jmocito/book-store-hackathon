package com.bookstore;

import com.bookstore.api.ApiExceptionMapper;
import com.bookstore.api.DocumentationResource;
import com.bookstore.api.HealthResource;
import com.bookstore.api.UnhandledExceptionMapper;
import com.bookstore.config.AppConfig;
import com.bookstore.config.RequestTracingFilter;
import com.bookstore.controller.BookController;
import com.bookstore.service.BookService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jsonb.JsonBindingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationPath("/api")
public class BookstoreApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookstoreApp.class);

    public static void main(String[] args) {
        AppConfig appConfig = AppConfig.load();
        MongoClient mongoClient = MongoClients.create(appConfig.mongoUri());
        BookService bookService = new BookService(mongoClient, appConfig.databaseName(), appConfig.collectionName());

        ResourceConfig rc = new ResourceConfig()
                .register(JsonBindingFeature.class)
                .register(new RequestTracingFilter())
                .register(new ApiExceptionMapper())
                .register(new UnhandledExceptionMapper())
                .register(new HealthResource())
                .register(new DocumentationResource(appConfig))
                .register(new BookController(bookService));

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(appConfig.baseUri(), rc);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(server, mongoClient)));

        LOGGER.info("bookstore api started baseUri={} environment={}", appConfig.baseUri(), appConfig.environment());
    }

    private static void shutdown(HttpServer server, MongoClient mongoClient) {
        LOGGER.info("shutting down bookstore api");
        server.shutdownNow();
        mongoClient.close();
    }
}
