package com.bookstore.controller;

import jakarta.json.bind.annotation.JsonbProperty;

import java.util.Date;

public class CreateBookDTO {
    private String title;
    private String authorId;
    private String author;
    @JsonbProperty("categoryId")
    private String legacyCategoryName;
    private String categoryName;
    private Integer quantity;
    private String description;
    private String language;

    private Boolean active;
    private Date inactiveDate;

    private String publisher;
    private Date publisherDate;

    public CreateBookDTO() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getCategoryId() {
        return legacyCategoryName;
    }

    public void setCategoryId(String categoryId) {
        this.legacyCategoryName = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isActive() {
        return active == null || active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Date getInactiveDate() {
        return inactiveDate;
    }

    public void setInactiveDate(Date inactiveDate) {
        this.inactiveDate = inactiveDate;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Date getPublisherDate() {
        return publisherDate;
    }

    public void setPublisherDate(Date publisherDate) {
        this.publisherDate = publisherDate;
    }

    public String resolveCategoryName() {
        if (categoryName != null && !categoryName.isBlank()) {
            return categoryName;
        }
        return legacyCategoryName;
    }
}
