package com.bookstore.controller;

import com.bookstore.model.Book;
import com.bookstore.service.BookService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BookControllerTest {

    @Test
    public void testCreateBookSuccess() {
        BookService mockService = Mockito.mock(BookService.class);

        Book created = new Book();
        created.setId("507f1f77bcf86cd799439011");
        created.setTitle("Test Driven Development");
        created.setAuthor("Kent Beck");
        created.setCategoryId("6");
        created.setDescription("A book about TDD");

        when(mockService.addBook("Test Driven Development", "Kent Beck", "6", "A book about TDD")).thenReturn(created);

        BookController controller = new BookController(mockService);

        Book request = new Book();
        request.setTitle("Test Driven Development");
        request.setAuthor("Kent Beck");
        request.setCategoryId("6");
        request.setDescription("A book about TDD");

        Response resp = controller.createBook(request);

        assertEquals(201, resp.getStatus());
        assertSame(created, resp.getEntity());
        verify(mockService).addBook("Test Driven Development", "Kent Beck", "6", "A book about TDD");
    }
}
