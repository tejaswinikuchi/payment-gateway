package com.gateway.controllers;

import com.gateway.models.Merchant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random();

    public PaymentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================
    // CREATE PAYMENT
    // =========================
    @PostMapping
    public ResponseEntity<?> createPayment(
            @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest
    ) {

        Merchant merchant = (Merchant) httpRequest.getAttribute("merchant");

        var orders = jdbcTemplate.queryForList(
                "SELECT amount, currency FROM orders WHERE id = ? AND merchant_id = ?",
                request.order_id, merchant.getId()
        );

        if (orders.isEmpty()) {
            return notFound("Order not found");
        }

        int amount = (int) orders.get(0).get("amount");
        String currency = orders.get(0).get("currency").toString();

        if (!"upi".equals(request.method) && !"card".equals(request.method)) {
            return badRequest("BAD_REQUEST_ERROR", "Invalid payment method");
        }

        String paymentId = generateId("pay_");

        // ---------- UPI ----------
        if ("upi".equals(request.method)) {

            if (request.vpa == null || !isValidVpa(request.vpa)) {
                return badRequest("INVALID_VPA", "Invalid VPA format");
            }

            jdbcTemplate.update("""
                INSERT INTO payments
                (id, order_id, merchant_id, amount, currency, method, status, vpa,
                 created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'upi', 'processing', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, paymentId, request.order_id, merchant.getId(), amount, currency, request.vpa);

            processPayment(paymentId, true);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "id", paymentId,
                            "order_id", request.order_id,
                            "amount", amount,
                            "currency", currency,
                            "method", "upi",
                            "vpa", request.vpa,
                            "status", "processing",
                            "created_at", Instant.now().toString()
                    )
            );
        }

        // ---------- CARD ----------
        Card card = request.card;

        if (card == null || !isValidCard(card)) {
            return badRequest("INVALID_CARD", "Card validation failed");
        }

        String network = detectNetwork(card.number);
        String last4 = card.number.substring(card.number.length() - 4);

        jdbcTemplate.update("""
            INSERT INTO payments
            (id, order_id, merchant_id, amount, currency, method, status,
             card_network, card_last4, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 'card', 'processing', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, paymentId, request.order_id, merchant.getId(), amount, currency, network, last4);

        processPayment(paymentId, false);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "id", paymentId,
                        "order_id", request.order_id,
                        "amount", amount,
                        "currency", currency,
                        "method", "card",
                        "card_network", network,
                        "card_last4", last4,
                        "status", "processing",
                        "created_at", Instant.now().toString()
                )
        );
    }

    // =========================
    // GET PAYMENT
    // =========================
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPayment(
            @PathVariable String paymentId,
            HttpServletRequest request
    ) {
        Merchant merchant = (Merchant) request.getAttribute("merchant");

        try {
            return ResponseEntity.ok(
                    jdbcTemplate.queryForObject("""
                        SELECT id, order_id, amount, currency, method, status,
                               vpa, card_network, card_last4,
                               created_at, updated_at
                        FROM payments
                        WHERE id = ? AND merchant_id = ?
                    """, new Object[]{paymentId, merchant.getId()}, (rs, rowNum) -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", rs.getString("id"));
                        map.put("order_id", rs.getString("order_id"));
                        map.put("amount", rs.getInt("amount"));
                        map.put("currency", rs.getString("currency"));
                        map.put("method", rs.getString("method"));
                        map.put("status", rs.getString("status"));

                        if ("upi".equals(rs.getString("method"))) {
                            map.put("vpa", rs.getString("vpa"));
                        } else {
                            map.put("card_network", rs.getString("card_network"));
                            map.put("card_last4", rs.getString("card_last4"));
                        }

                        map.put("created_at",
                                rs.getTimestamp("created_at").toInstant().toString());
                        map.put("updated_at",
                                rs.getTimestamp("updated_at").toInstant().toString());
                        return map;
                    })
            );
        } catch (Exception e) {
            return notFound("Payment not found");
        }
    }

    // =========================
    // PROCESSING
    // =========================
    private void processPayment(String paymentId, boolean upi) {
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        boolean success = upi ? random.nextInt(10) < 9 : random.nextInt(100) < 95;

        if (success) {
            jdbcTemplate.update(
                    "UPDATE payments SET status='success', updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    paymentId
            );
        } else {
            jdbcTemplate.update("""
                UPDATE payments
                SET status='failed',
                    error_code='PAYMENT_FAILED',
                    error_description='Bank declined transaction',
                    updated_at=CURRENT_TIMESTAMP
                WHERE id=?
            """, paymentId);
        }
    }

    // =========================
    // VALIDATION
    // =========================
    private boolean isValidVpa(String vpa) {
        return Pattern.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$", vpa);
    }

    private boolean isValidCard(Card card) {
        return card.number != null
                && luhn(card.number)
                && card.expiry_month != null
                && card.expiry_year != null
                && card.cvv != null;
    }

    private boolean luhn(String number) {
        int sum = 0;
        boolean alt = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    private String detectNetwork(String number) {
        if (number.startsWith("4")) return "visa";
        if (number.startsWith("5")) return "mastercard";
        if (number.startsWith("34") || number.startsWith("37")) return "amex";
        if (number.startsWith("6")) return "rupay";
        return "unknown";
    }

    // =========================
    // UTIL
    // =========================
    private String generateId(String prefix) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private ResponseEntity<?> badRequest(String code, String msg) {
        return ResponseEntity.badRequest().body(
                Map.of("error", Map.of("code", code, "description", msg))
        );
    }

    private ResponseEntity<?> notFound(String msg) {
        return ResponseEntity.status(404).body(
                Map.of("error", Map.of("code", "NOT_FOUND_ERROR", "description", msg))
        );
    }

    // =========================
    // DTOs
    // =========================
    static class PaymentRequest {
        public String order_id;
        public String method;
        public String vpa;
        public Card card;
    }

    static class Card {
        public String number;
        public String expiry_month;
        public String expiry_year;
        public String cvv;
        public String holder_name;
    }
}
