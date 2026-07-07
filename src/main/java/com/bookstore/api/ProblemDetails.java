package com.bookstore.api;

public record ProblemDetails(
        String type,
        String title,
        int status,
        String detail,
        String instance) {

    public static ProblemDetails from(String type, String title, int status, String detail, String traceId) {
        return new ProblemDetails(type, title, status, detail, "urn:trace:" + traceId);
    }
}
