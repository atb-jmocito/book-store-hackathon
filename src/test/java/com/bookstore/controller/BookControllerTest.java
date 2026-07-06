package com.bookstore.controller;

import com.bookstore.model.Book;
import com.bookstore.service.BookService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BookControllerTest {

    @Test
    public void testCreateBookSuccess() {
        BookService mockService = Mockito.mock(BookService.class);

        Date inactiveDate = new Date(0);
        Date publisherDate = new Date(1);

        Book created = new Book();
        created.setId("507f1f77bcf86cd799439011");
        created.setTitle("Test Driven Development");
        created.setAuthor("Kent Beck");
        created.setCategoryId(6);
        created.setQuantity(3);
        created.setDescription("A book about TDD");
        created.setLanguage("English");
        created.setActive(true);
        created.setInactiveDate(inactiveDate);
        created.setPublisher("Addison-Wesley");
        created.setPublisherDate(publisherDate);

        String categoryName = "Programming";

        when(mockService.upsertBook(
                null,
                "Test Driven Development",
                "Kent Beck",
                categoryName,
                3,
                "A book about TDD",
                "English",
                true,
                inactiveDate,
                "Addison-Wesley",
                publisherDate)).thenReturn(created);

        BookController controller = new BookController(mockService);

        CreateBookDTO request = new CreateBookDTO();
        request.setTitle("Test Driven Development");
        request.setAuthor("Kent Beck");
        request.setCategoryId(categoryName);
        request.setQuantity(3);
        request.setDescription("A book about TDD");
        request.setLanguage("English");
        request.setActive(true);
        request.setInactiveDate(inactiveDate);
        request.setPublisher("Addison-Wesley");
        request.setPublisherDate(publisherDate);

        Response resp = controller.createBook(request);

        assertEquals(201, resp.getStatus());
        assertSame(created, resp.getEntity());
        verify(mockService).upsertBook(
                null,
                "Test Driven Development",
                "Kent Beck",
                categoryName,
                3,
                "A book about TDD",
                "English",
                true,
                inactiveDate,
                "Addison-Wesley",
                publisherDate);
    }
}
