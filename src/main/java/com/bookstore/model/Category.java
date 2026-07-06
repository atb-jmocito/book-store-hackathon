package com.bookstore.model;

import java.util.ArrayList;
import java.util.List;

public class Category {
    private String id;
    private String name;

    // Hardcoded categories (intentional code smell)
    public static final List<Category> STATIC_CATEGORIES = new ArrayList<>();

    static {
        STATIC_CATEGORIES.add(new Category("1", "Fiction"));
        STATIC_CATEGORIES.add(new Category("2", "Non-Fiction"));
        STATIC_CATEGORIES.add(new Category("3", "Science"));
        STATIC_CATEGORIES.add(new Category("4", "Biography"));
        STATIC_CATEGORIES.add(new Category("5", "Children"));
        STATIC_CATEGORIES.add(new Category("6", "Programming"));
    }

    public Category() {}

    public Category(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
