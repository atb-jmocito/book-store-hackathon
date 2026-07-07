package com.bookstore.controller;

import com.bookstore.api.HealthResource;
import com.bookstore.config.RequestTracingFilter;
import com.bookstore.model.Book;
import com.bookstore.service.BookService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response;

@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GET
    @Path("/health")
    public Response health() {
        return deprecated(Response.ok(new HealthResource.HealthStatus("UP", RequestTracingFilter.currentTraceId())));
    }

    @GET
    public Response listBooks(@QueryParam("category") String category) {
        return Response.ok(bookService.listBooks(category)).build();
    }

    @GET
    @Path("/single")
    public Response getLegacyBook(@QueryParam("id") String id, @QueryParam("name") String name, @QueryParam("author") String author) {
        return deprecated(Response.ok(bookService.getSingleBook(id, name, author)));
    }

    @GET
    @Path("/{id}")
    public Response getBookById(@PathParam("id") String id) {
        return Response.ok(bookService.getBookById(id)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateBook(@PathParam("id") String id, UpdateBookDTO request) {
        Book updated = bookService.updateBook(id, request);
        return Response.ok(updated).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateLegacyBook(UpdateBookDTO request) {
        Book updated = bookService.updateBook(request == null ? null : request.getId(), request);
        return deprecated(Response.ok(updated));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBook(CreateBookDTO request) {
        Book created = bookService.createBook(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    private Response deprecated(ResponseBuilder responseBuilder) {
        return responseBuilder
                .header("Deprecation", "true")
                .header("Warning", "299 - Deprecated endpoint, prefer documented replacement in OpenAPI")
                .build();
    }
}
