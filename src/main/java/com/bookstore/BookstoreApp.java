package com.bookstore;

import com.bookstore.controller.BookController;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jsonb.JsonBindingFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

@ApplicationPath("/api")
public class BookstoreApp {
    public static void main(String[] args) {
        // Hardcoded base URI and port (intentional)
        String base = "http://0.0.0.0:8080/api/";
        ResourceConfig rc = new ResourceConfig()
                .register(JsonBindingFeature.class)
                .register(BookController.class);

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(base), rc);
        System.out.println("Bookstore API started at " + base);
        // no graceful shutdown handling
    }
}
