package com.gateway.controllers;

import com.gateway.models.Merchant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random();

    public OrderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================
    // CREATE ORDER
    // =========================
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequest request,
            HttpServletRequest httpRequest
    ) {

        Merchant merchant = (Merchant) httpRequest.getAttribute("merchant");

        if (merchant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", Map.of(
                            "code", "AUTHENTICATION_ERROR",
                            "description", "Invalid API credentials"
                    ))
            );
        }

        if (request.amount == null || request.amount < 100) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", Map.of(
                            "code", "BAD_REQUEST_ERROR",
                            "description", "amount must be at least 100"
                    ))
            );
        }

        String orderId = generateId("order_");

        jdbcTemplate.update("""
            INSERT INTO orders
            (id, merchant_id, amount, currency, status, created_at, updated_at)
            VALUES (?, ?, ?, 'INR', 'created', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, orderId, merchant.getId(), request.amount);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "id", orderId,
                        "merchant_id", merchant.getId().toString(),
                        "amount", request.amount,
                        "currency", "INR",
                        "status", "created",
                        "created_at", Instant.now().toString()
                )
        );
    }

    // =========================
    // PUBLIC ORDER FETCH
    // =========================
    @GetMapping("/{orderId}/public")
    public ResponseEntity<?> getPublicOrder(@PathVariable String orderId) {

        var rows = jdbcTemplate.queryForList(
                "SELECT id, amount, currency, status FROM orders WHERE id = ?",
                orderId
        );

        if (rows.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", Map.of(
                            "code", "NOT_FOUND_ERROR",
                            "description", "Order not found"
                    ))
            );
        }

        var row = rows.get(0);

        return ResponseEntity.ok(
                Map.of(
                        "id", row.get("id"),
                        "amount", row.get("amount"),
                        "currency", row.get("currency"),
                        "status", row.get("status")
                )
        );
    }

    // =========================
    // ID GENERATOR
    // =========================
    private String generateId(String prefix) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    static class OrderRequest {
        public Integer amount;
    }
}
