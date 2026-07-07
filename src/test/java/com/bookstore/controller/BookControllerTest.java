package com.bookstore.controller;

import com.bookstore.model.Book;
import com.bookstore.service.BookService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BookControllerTest {
    private BookService mockService;
    private BookController controller;

    @BeforeEach
    void setUp() {
        mockService = Mockito.mock(BookService.class);
        controller = new BookController(mockService);
    }

    @Test
    public void testCreateBookSuccess() {
        Book created = new Book();
        created.setId("507f1f77bcf86cd799439011");
        created.setTitle("Test Driven Development");

        CreateBookDTO request = new CreateBookDTO();
        request.setTitle("Test Driven Development");
        request.setCategoryName("Programming");

        when(mockService.createBook(request)).thenReturn(created);

        Response resp = controller.createBook(request);

        assertEquals(201, resp.getStatus());
        assertSame(created, resp.getEntity());
        verify(mockService).createBook(request);
    }

    @Test
    public void testLegacyLookupAddsDeprecationHeader() {
        Book book = new Book();
        book.setId("507f1f77bcf86cd799439011");

        when(mockService.getSingleBook("507f1f77bcf86cd799439011", null, null)).thenReturn(book);

        Response response = controller.getLegacyBook("507f1f77bcf86cd799439011", null, null);

        assertEquals(200, response.getStatus());
        assertEquals("true", response.getHeaderString("Deprecation"));
        assertSame(book, response.getEntity());
    }

    @Test
    public void testListBooksDelegatesToService() {
        Book book = new Book();
        book.setTitle("Clean Code");
        when(mockService.listBooks("Programming")).thenReturn(List.of(book));

        Response response = controller.listBooks("Programming");

        assertEquals(200, response.getStatus());
        assertEquals(List.of(book), response.getEntity());
        verify(mockService).listBooks("Programming");
    }

    @Test
    public void testGetBookByIdDelegatesToService() {
        Book book = new Book();
        book.setId("507f1f77bcf86cd799439011");
        when(mockService.getBookById(book.getId())).thenReturn(book);

        Response response = controller.getBookById(book.getId());

        assertEquals(200, response.getStatus());
        assertSame(book, response.getEntity());
        verify(mockService).getBookById(book.getId());
    }

    @Test
    public void testUpdateBookReturnsOk() {
        UpdateBookDTO request = new UpdateBookDTO();
        request.setQuantity(4);
        Book updated = new Book();
        updated.setQuantity(4);
        when(mockService.updateBook("507f1f77bcf86cd799439011", request)).thenReturn(updated);

        Response response = controller.updateBook("507f1f77bcf86cd799439011", request);

        assertEquals(200, response.getStatus());
        assertSame(updated, response.getEntity());
        verify(mockService).updateBook("507f1f77bcf86cd799439011", request);
    }

    @Test
    public void testUpdateLegacyBookAddsDeprecationHeader() {
        UpdateBookDTO request = new UpdateBookDTO();
        request.setId("507f1f77bcf86cd799439011");
        Book updated = new Book();
        when(mockService.updateBook("507f1f77bcf86cd799439011", request)).thenReturn(updated);

        Response response = controller.updateLegacyBook(request);

        assertEquals(200, response.getStatus());
        assertEquals("true", response.getHeaderString("Deprecation"));
        assertSame(updated, response.getEntity());
    }

    @Test
    public void testHealthAddsDeprecationHeader() {
        Response response = controller.health();

        assertEquals(200, response.getStatus());
        assertEquals("true", response.getHeaderString("Deprecation"));
    }
}
