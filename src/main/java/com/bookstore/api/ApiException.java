package com.bookstore.api;

import jakarta.ws.rs.core.Response;

public class ApiException extends RuntimeException {
    private final Response.Status status;
    private final String type;
    private final String title;

    public ApiException(Response.Status status, String type, String title, String detail) {
        super(detail);
        this.status = status;
        this.type = type;
        this.title = title;
    }

    public Response.Status status() {
        return status;
    }

    public String type() {
        return type;
    }

    public String title() {
        return title;
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(Response.Status.BAD_REQUEST, "https://bookstore.dev/problems/bad-request", "Bad Request", detail);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(Response.Status.NOT_FOUND, "https://bookstore.dev/problems/not-found", "Not Found", detail);
    }
}
