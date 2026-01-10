package com.gateway.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final JdbcTemplate jdbcTemplate;

    public PaymentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================
    // CREATE PAYMENT
    // =========================
    @PostMapping
    public ResponseEntity<?> createPayment(@RequestBody PaymentRequest request) {

        if (request.order_id == null || request.method == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "order_id and method are required")
            );
        }

        var orders = jdbcTemplate.queryForList(
                "SELECT id, amount, currency, merchant_id FROM orders WHERE id = ?",
                request.order_id
        );

        if (orders.isEmpty()) {
            return ResponseEntity.status(404).body(
                    Map.of("error", "Order not found")
            );
        }

        var order = orders.get(0);
        String paymentId = generateId("pay_");

        jdbcTemplate.update("""
            INSERT INTO payments
            (id, order_id, merchant_id, amount, currency, method, status, vpa, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, 'processing', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
                paymentId,
                request.order_id,
                order.get("merchant_id"),
                order.get("amount"),
                order.get("currency"),
                request.method,
                request.vpa
        );

        return ResponseEntity.status(201).body(
                Map.of(
                        "id", paymentId,
                        "order_id", request.order_id,
                        "status", "processing",
                        "method", request.method,
                        "created_at", Instant.now().toString()
                )
        );
    }

    // =========================
    // CONFIRM PAYMENT (FINAL STEP)
    // =========================
    @PostMapping("/{paymentId}/confirm")
    public ResponseEntity<?> confirmPayment(@PathVariable String paymentId) {

        int updated = jdbcTemplate.update(
                "UPDATE payments SET status = 'success', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                paymentId
        );

        if (updated == 0) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Payment not found"));
        }

        jdbcTemplate.update("""
            UPDATE orders
            SET status = 'paid', updated_at = CURRENT_TIMESTAMP
            WHERE id = (SELECT order_id FROM payments WHERE id = ?)
        """, paymentId);

        return ResponseEntity.ok(
                Map.of(
                        "id", paymentId,
                        "status", "success"
                )
        );
    }

    // =========================
    // UTIL
    // =========================
    private String generateId(String prefix) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // =========================
    // REQUEST DTO
    // =========================
    static class PaymentRequest {
        public String order_id;
        public String method;
        public String vpa;
    }
}
