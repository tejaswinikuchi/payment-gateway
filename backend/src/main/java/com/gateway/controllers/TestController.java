package com.gateway.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TestController {

    private final JdbcTemplate jdbcTemplate;

    public TestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/v1/test/merchant")
    public ResponseEntity<?> getTestMerchant() {

        List<Map<String, Object>> result = jdbcTemplate.queryForList(
                "SELECT id, email, api_key FROM merchants WHERE email = ?",
                "test@example.com"
        );

        if (result.isEmpty()) {
            return ResponseEntity.status(404).body(
                    Map.of(
                            "error", "Test merchant not found",
                            "seeded", false
                    )
            );
        }

        Map<String, Object> row = result.get(0);

        return ResponseEntity.ok(
                Map.of(
                        "id", row.get("id"),
                        "email", row.get("email"),
                        "api_key", row.get("api_key"),
                        "seeded", true
                )
        );
    }
}
