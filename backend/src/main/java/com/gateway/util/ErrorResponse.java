package com.gateway.util;

import java.util.Map;

public class ErrorResponse {

    public static Map<String, Object> authError() {
        return Map.of(
                "error", Map.of(
                        "code", "AUTHENTICATION_ERROR",
                        "description", "Invalid API credentials"
                )
        );
    }

    public static Map<String, Object> badRequest(String description) {
        return Map.of(
                "error", Map.of(
                        "code", "BAD_REQUEST_ERROR",
                        "description", description
                )
        );
    }

    public static Map<String, Object> notFound(String description) {
        return Map.of(
                "error", Map.of(
                        "code", "NOT_FOUND_ERROR",
                        "description", description
                )
        );
    }

   
    public static Map<String, Object> custom(String code, String description) {
        return Map.of(
                "error", Map.of(
                        "code", code,
                        "description", description
                )
        );
    }
}
