package com.bookstore.controller;

public class UpdateBookDTO {
    private String Id;
    private int quantity;
    private boolean active;

    public UpdateBookDTO() {}

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
