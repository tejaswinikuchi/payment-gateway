package com.gateway.controllers;

import com.gateway.util.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final JdbcTemplate jdbcTemplate;

    public OrderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader("X-Api-Key") String apiKey,
            @RequestHeader("X-Api-Secret") String apiSecret,
            @RequestBody Map<String, Object> body
    ) {
        // ---------- Amount validation ----------
        Object amountObj = body.get("amount");
        if (amountObj == null) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.badRequest("amount must be at least 100"));
        }

        int amount = ((Number) amountObj).intValue();
        if (amount < 100) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.badRequest("amount must be at least 100"));
        }

        String currency = (String) body.getOrDefault("currency", "INR");
        String receipt = (String) body.get("receipt");

        // ---------- Merchant lookup (UUID SAFE) ----------
        List<UUID> merchants = jdbcTemplate.queryForList(
                "SELECT id FROM merchants WHERE api_key = ? AND api_secret = ?",
                UUID.class,
                apiKey,
                apiSecret
        );

        if (merchants.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.authError());
        }

        UUID merchantId = merchants.get(0);

        // ---------- Create order ----------
        String orderId = generateId("order_");

        jdbcTemplate.update("""
            INSERT INTO orders
            (id, merchant_id, amount, currency, receipt, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 'created', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, orderId, merchantId, amount, currency, receipt);

        return ResponseEntity.status(201).body(
                Map.of(
                        "id", orderId,
                        "merchant_id", merchantId.toString(),
                        "amount", amount,
                        "currency", currency,
                        "receipt", receipt,
                        "status", "created",
                        "created_at", Instant.now().toString()
                )
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM orders WHERE id = ?",
                orderId
        );

        if (rows.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(ErrorResponse.notFound("Order not found"));
        }

        return ResponseEntity.ok(rows.get(0));
    }

    private String generateId(String prefix) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(prefix);
        Random random = new Random();
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
