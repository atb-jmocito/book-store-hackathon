package com.bookstore.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class Category {
    private int id;
    private String name;

    public static final List<Category> STATIC_CATEGORIES = List.of(
            new Category(1, "Fiction"),
            new Category(2, "Non-Fiction"),
            new Category(3, "Science"),
            new Category(4, "Biography"),
            new Category(5, "Children"),
            new Category(6, "Programming"));
    private static final Map<String, Category> CATEGORIES_BY_NAME = categoriesByName();

    public Category() {}

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static Optional<Category> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(CATEGORIES_BY_NAME.get(name.trim().toLowerCase(Locale.ROOT)));
    }

    private static Map<String, Category> categoriesByName() {
        Map<String, Category> categories = new LinkedHashMap<>();
        for (Category category : STATIC_CATEGORIES) {
            categories.put(category.getName().toLowerCase(Locale.ROOT), category);
        }
        return Map.copyOf(categories);
    }
}
