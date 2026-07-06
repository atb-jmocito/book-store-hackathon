package com.bookstore.controller;

import com.bookstore.model.Book;
import com.bookstore.service.BookService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
public class BookController {
    private BookService bookService;

    public BookController() {
        // direct instantiation (tight coupling)
        this.bookService = new BookService();
    }

    // constructor for tests to inject a mock service
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listBook() {
        var result = bookService.listBooks();
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBook(Book request) {
        // no null checks, potential NPE if request or fields are null
        Book created = bookService.addBook(request.getTitle(), request.getAuthor(), request.getCategoryId(), request.getDescription());
        return Response.status(Response.Status.CREATED).entity(created).build();
    }
}
