package com.gateway.controllers;

import com.gateway.controllers.PaymentController.Card;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random();

    public CheckoutController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================
    // PUBLIC PAY (NO AUTH)
    // =========================
    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestBody CheckoutRequest request) {

        // 1. Order lookup
        var orders = jdbcTemplate.queryForList(
                "SELECT amount, currency, merchant_id FROM orders WHERE id = ?",
                request.order_id
        );

        if (orders.isEmpty()) {
            return notFound("Order not found");
        }

        int amount = (int) orders.get(0).get("amount");
        String currency = orders.get(0).get("currency").toString();
        Object merchantId = orders.get(0).get("merchant_id");

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
            """, paymentId, request.order_id, merchantId, amount, currency, request.vpa);

            process(paymentId, true);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "id", paymentId,
                            "status", "processing",
                            "method", "upi",
                            "amount", amount,
                            "currency", currency,
                            "created_at", Instant.now().toString()
                    )
            );
        }

        // ---------- CARD ----------
        Card card = request.card;

        if (card == null || !luhn(card.number)) {
            return badRequest("INVALID_CARD", "Card validation failed");
        }

        String network = detect(card.number);
        String last4 = card.number.substring(card.number.length() - 4);

        jdbcTemplate.update("""
            INSERT INTO payments
            (id, order_id, merchant_id, amount, currency, method, status,
             card_network, card_last4, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 'card', 'processing', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """, paymentId, request.order_id, merchantId, amount, currency, network, last4);

        process(paymentId, false);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "id", paymentId,
                        "status", "processing",
                        "method", "card",
                        "amount", amount,
                        "currency", currency,
                        "card_network", network,
                        "card_last4", last4,
                        "created_at", Instant.now().toString()
                )
        );
    }

    // =========================
    // PROCESS
    // =========================
    private void process(String paymentId, boolean upi) {
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        boolean success = upi ? random.nextInt(10) < 9 : random.nextInt(100) < 95;

        jdbcTemplate.update(
                "UPDATE payments SET status=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                success ? "success" : "failed", paymentId
        );
    }

    // =========================
    // VALIDATION
    // =========================
    private boolean isValidVpa(String vpa) {
        return Pattern.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$", vpa);
    }

    private boolean luhn(String n) {
        int sum = 0; boolean alt = false;
        for (int i = n.length() - 1; i >= 0; i--) {
            int d = n.charAt(i) - '0';
            if (alt) { d *= 2; if (d > 9) d -= 9; }
            sum += d; alt = !alt;
        }
        return sum % 10 == 0;
    }

    private String detect(String n) {
        if (n.startsWith("4")) return "visa";
        if (n.startsWith("5")) return "mastercard";
        if (n.startsWith("6")) return "rupay";
        return "unknown";
    }

    private String generateId(String p) {
        String c = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder s = new StringBuilder(p);
        for (int i = 0; i < 16; i++) s.append(c.charAt(random.nextInt(c.length())));
        return s.toString();
    }

    private ResponseEntity<?> badRequest(String c, String m) {
        return ResponseEntity.badRequest().body(
                Map.of("error", Map.of("code", c, "description", m))
        );
    }

    private ResponseEntity<?> notFound(String m) {
        return ResponseEntity.status(404).body(
                Map.of("error", Map.of("code", "NOT_FOUND_ERROR", "description", m))
        );
    }

    // =========================
    // DTO
    // =========================
    static class CheckoutRequest {
        public String order_id;
        public String method;
        public String vpa;
        public Card card;
    }
}
