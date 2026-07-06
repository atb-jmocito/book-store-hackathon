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
        this.bookService = new BookService();
    }

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
    public Response listBook(@QueryParam("category") String category) {
        var result = bookService.getBookList(category);
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/single")
    public Response getBook(@QueryParam("id") String Id, @QueryParam("name") String name, @QueryParam("author") String author) {
        if (author != null) {
            // TODO endpoint already prepared for new filter. When you develop the author support create the filter by author
            throw new RuntimeException("Filtering by author is not implemented yet");
        }

        var result = bookService.getBook(Id, name);
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateBook(UpdateBookDTO request) {
        if (request.getId() != null) {
            Book created = bookService.upsertBook(
                    request.getId(),
                    null,
                    null,
                    null,
                    request.getQuantity(),
                    null,
                    null,
                    request.isActive(),
                    null,
                    null,
                    null);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBook(CreateBookDTO request) {
        if (request.getTitle() != null) {
            Book created = bookService.upsertBook(
                    null,
                    request.getTitle(),
                    request.getAuthor(),
                    request.getCategoryId(),
                    request.getQuantity(),
                    request.getDescription(),
                    request.getLanguage(),
                    request.isActive(),
                    request.getInactiveDate(),
                    request.getPublisher(),
                    request.getPublisherDate());
            return Response.status(Response.Status.CREATED).entity(created).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
