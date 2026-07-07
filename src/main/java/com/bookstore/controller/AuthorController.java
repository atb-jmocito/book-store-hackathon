package com.bookstore.controller;

import com.bookstore.model.Author;
import com.bookstore.service.AuthorService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/authors")
@Produces(MediaType.APPLICATION_JSON)
public class AuthorController {
    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GET
    public Response listAuthors(@QueryParam("active") Boolean active) {
        List<Author> authors = authorService.listAuthors(active);
        return Response.ok(authors).build();
    }

    @GET
    @Path("/{id}")
    public Response getAuthorById(@PathParam("id") String id) {
        return Response.ok(authorService.getAuthorById(id)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAuthor(CreateAuthorDTO request) {
        Author created = authorService.createAuthor(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteAuthor(@PathParam("id") String id) {
        return Response.ok(authorService.softDeleteAuthor(id)).build();
    }
}
