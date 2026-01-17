package com.gateway.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();

        response.put("status", "healthy");
        response.put("timestamp", Instant.now().toString());

        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                response.put("database", "connected");
            } else {
                response.put("database", "disconnected");
            }
        } catch (Exception e) {
            response.put("database", "disconnected");
        }

        return ResponseEntity.ok(response);
    }
}
