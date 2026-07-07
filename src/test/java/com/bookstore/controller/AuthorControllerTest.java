package com.bookstore.controller;

import com.bookstore.model.Author;
import com.bookstore.service.AuthorService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorControllerTest {
    private AuthorService mockService;
    private AuthorController controller;

    @BeforeEach
    void setUp() {
        mockService = Mockito.mock(AuthorService.class);
        controller = new AuthorController(mockService);
    }

    @Test
    void createAuthorReturnsCreatedResponse() {
        CreateAuthorDTO request = new CreateAuthorDTO();
        request.setName("Robert C. Martin");
        Author created = new Author();
        created.setId("507f1f77bcf86cd799439011");
        created.setName("Robert C. Martin");
        when(mockService.createAuthor(request)).thenReturn(created);

        Response response = controller.createAuthor(request);

        assertEquals(201, response.getStatus());
        assertSame(created, response.getEntity());
        verify(mockService).createAuthor(request);
    }

    @Test
    void listAuthorsDelegatesToService() {
        Author author = new Author();
        author.setName("Robert C. Martin");
        when(mockService.listAuthors(true)).thenReturn(List.of(author));

        Response response = controller.listAuthors(true);

        assertEquals(200, response.getStatus());
        assertEquals(List.of(author), response.getEntity());
        verify(mockService).listAuthors(true);
    }

    @Test
    void getAuthorByIdDelegatesToService() {
        Author author = new Author();
        author.setId("507f1f77bcf86cd799439011");
        when(mockService.getAuthorById(author.getId())).thenReturn(author);

        Response response = controller.getAuthorById(author.getId());

        assertEquals(200, response.getStatus());
        assertSame(author, response.getEntity());
        verify(mockService).getAuthorById(author.getId());
    }

    @Test
    void deleteAuthorDelegatesToService() {
        Author author = new Author();
        author.setId("507f1f77bcf86cd799439011");
        author.setActive(false);
        when(mockService.softDeleteAuthor(author.getId())).thenReturn(author);

        Response response = controller.deleteAuthor(author.getId());

        assertEquals(200, response.getStatus());
        assertSame(author, response.getEntity());
        verify(mockService).softDeleteAuthor(author.getId());
    }
}
