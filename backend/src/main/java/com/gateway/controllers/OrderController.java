package com.gateway.controllers;

import com.gateway.models.Merchant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final JdbcTemplate jdbcTemplate;

    public OrderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================
    // CREATE ORDER (AUTH)
    // =========================
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequest request,
            HttpServletRequest httpRequest
    ) {

        if (request.amount == null || request.amount < 100) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", Map.of(
                            "code", "BAD_REQUEST_ERROR",
                            "description", "amount must be at least 100"
                    ))
            );
        }

        Merchant merchant = (Merchant) httpRequest.getAttribute("merchant");

        String orderId = generateId("order_");

        jdbcTemplate.update("""
            INSERT INTO orders
            (id, merchant_id, amount, currency, receipt, notes, status, created_at, updated_at)
            VALUES (?, ?, ?, 'INR', ?, ?::jsonb, 'created', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
                orderId,
                merchant.getId(),
                request.amount,
                request.receipt,
                request.notes == null ? "{}" : request.notes.toString()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "id", orderId,
                        "merchant_id", merchant.getId().toString(),
                        "amount", request.amount,
                        "currency", "INR",
                        "receipt", request.receipt,
                        "notes", request.notes == null ? Map.of() : request.notes,
                        "status", "created",
                        "created_at", Instant.now().toString()
                )
        );
    }

    // =========================
    // GET ORDER (AUTH)
    // =========================
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(
            @PathVariable String orderId,
            HttpServletRequest request
    ) {

        Merchant merchant = (Merchant) request.getAttribute("merchant");

        try {
            return ResponseEntity.ok(
                    jdbcTemplate.queryForObject(
                            """
                            SELECT id, merchant_id, amount, currency, receipt, notes,
                                   status, created_at, updated_at
                            FROM orders
                            WHERE id = ? AND merchant_id = ?
                            """,
                            new Object[]{orderId, merchant.getId()},
                            (rs, rowNum) -> {
                                Map<String, Object> map = new HashMap<>();
                                map.put("id", rs.getString("id"));
                                map.put("merchant_id", rs.getString("merchant_id"));
                                map.put("amount", rs.getInt("amount"));
                                map.put("currency", rs.getString("currency"));
                                map.put("receipt", rs.getString("receipt"));
                                map.put("notes", rs.getObject("notes"));
                                map.put("status", rs.getString("status"));
                                map.put("created_at", rs.getTimestamp("created_at").toInstant().toString());
                                map.put("updated_at", rs.getTimestamp("updated_at").toInstant().toString());
                                return map;
                            }
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", Map.of(
                            "code", "NOT_FOUND_ERROR",
                            "description", "Order not found"
                    ))
            );
        }
    }

    // =========================
    // PUBLIC ORDER (NO AUTH)
    // =========================
    @GetMapping("/{orderId}/public")
    public Map<String, Object> getPublicOrder(@PathVariable String orderId) {

        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT id, amount, currency, status
                    FROM orders
                    WHERE id = ?
                    """,
                    new Object[]{orderId},
                    (rs, rowNum) -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", rs.getString("id"));
                        map.put("amount", rs.getInt("amount"));
                        map.put("currency", rs.getString("currency"));
                        map.put("status", rs.getString("status"));
                        return map;
                    }
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
    }

    // =========================
    // ID GENERATOR
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
    static class OrderRequest {
        public Integer amount;
        public String receipt;
        public Map<String, Object> notes;
    }
}
