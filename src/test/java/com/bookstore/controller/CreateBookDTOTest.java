package com.bookstore.controller;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateBookDTOTest {
    @Test
    void resolveCategoryNamePrefersNewFieldAndDefaultsActive() {
        CreateBookDTO dto = new CreateBookDTO();
        Date inactiveDate = new Date(0);
        Date publisherDate = new Date(1);

        dto.setTitle("Refactoring");
        dto.setAuthorId("507f1f77bcf86cd799439011");
        dto.setAuthor("Martin Fowler");
        dto.setCategoryId("Programming");
        dto.setCategoryName("Science");
        dto.setQuantity(5);
        dto.setDescription("Refactoring techniques");
        dto.setLanguage("English");
        dto.setInactiveDate(inactiveDate);
        dto.setPublisher("Addison-Wesley");
        dto.setPublisherDate(publisherDate);

        assertEquals("Refactoring", dto.getTitle());
        assertEquals("507f1f77bcf86cd799439011", dto.getAuthorId());
        assertEquals("Martin Fowler", dto.getAuthor());
        assertEquals("Programming", dto.getCategoryId());
        assertEquals("Science", dto.getCategoryName());
        assertEquals(Integer.valueOf(5), dto.getQuantity());
        assertEquals("Refactoring techniques", dto.getDescription());
        assertEquals("English", dto.getLanguage());
        assertTrue(dto.isActive());
        assertEquals(inactiveDate, dto.getInactiveDate());
        assertEquals("Addison-Wesley", dto.getPublisher());
        assertEquals(publisherDate, dto.getPublisherDate());
        assertEquals("Science", dto.resolveCategoryName());
    }

    @Test
    void resolveCategoryNameFallsBackToLegacyAlias() {
        CreateBookDTO dto = new CreateBookDTO();
        dto.setCategoryId("Programming");
        dto.setCategoryName(" ");
        dto.setActive(false);

        assertEquals("Programming", dto.resolveCategoryName());
        assertEquals(false, dto.isActive());
    }

    @Test
    void resolveCategoryNameReturnsNullWhenUnset() {
        CreateBookDTO dto = new CreateBookDTO();

        assertNull(dto.resolveCategoryName());
    }
}
