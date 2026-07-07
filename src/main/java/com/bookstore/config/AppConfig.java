package com.bookstore.config;

import java.net.URI;

public record AppConfig(
        String host,
        int port,
        String apiBasePath,
        String mongoUri,
        String databaseName,
        String collectionName,
        String environment,
        boolean swaggerUiEnabled) {

    public static AppConfig load() {
        String environment = envOrDefault("APP_ENV", "local");
        boolean swaggerUiEnabled = !"production".equalsIgnoreCase(environment)
                && Boolean.parseBoolean(envOrDefault("ENABLE_SWAGGER_UI", "true"));

        return new AppConfig(
                envOrDefault("SERVER_HOST", "0.0.0.0"),
                intEnvOrDefault("SERVER_PORT", 8080),
                normalizeBasePath(envOrDefault("API_BASE_PATH", "/api")),
                envOrDefault("MONGODB_URI", "mongodb://mongodb:27017"),
                envOrDefault("BOOKSTORE_DATABASE", "bookstore"),
                envOrDefault("BOOKSTORE_COLLECTION", "books"),
                environment,
                swaggerUiEnabled);
    }

    public URI baseUri() {
        return URI.create("http://" + host + ":" + port + apiBasePath + "/");
    }

    public String apiBasePathWithoutTrailingSlash() {
        return apiBasePath;
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int intEnvOrDefault(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(key + " must be a valid integer", exception);
        }
    }

    private static String normalizeBasePath(String basePath) {
        String trimmed = basePath.trim();
        String withLeadingSlash = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return withLeadingSlash.endsWith("/") ? withLeadingSlash.substring(0, withLeadingSlash.length() - 1) : withLeadingSlash;
    }
}
