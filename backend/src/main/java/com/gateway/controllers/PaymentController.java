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
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random();

    public PaymentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    public ResponseEntity<?> createPayment(
            @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest
    ) {

        Merchant merchant = (Merchant) httpRequest.getAttribute("merchant");

        if (merchant == null) {
            return ResponseEntity.status(401).body(
                    Map.of("error", Map.of(
                            "code", "AUTHENTICATION_ERROR",
                            "description", "Invalid API credentials"
                    ))
            );
        }

        var orders = jdbcTemplate.queryForList(
                "SELECT amount, currency FROM orders WHERE id = ? AND merchant_id = ?",
                request.order_id, merchant.getId()
        );

        if (orders.isEmpty()) {
            return ResponseEntity.status(404).body(
                    Map.of("error", Map.of(
                            "code", "NOT_FOUND_ERROR",
                            "description", "Order not found"
                    ))
            );
        }

        int amount = (int) orders.get(0).get("amount");
        String currency = orders.get(0).get("currency").toString();

        String paymentId = generateId("pay_");

        if ("upi".equals(request.method)) {

            if (request.vpa == null || !isValidVpa(request.vpa)) {
                return badRequest("INVALID_VPA", "Invalid VPA format");
            }

            jdbcTemplate.update("""
                INSERT INTO payments
                (id, order_id, merchant_id, amount, currency, method, status, vpa, created_at, updated_at)
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

        if ("card".equals(request.method)) {

            if (request.card == null || !isValidCard(request.card)) {
                return badRequest("INVALID_CARD", "Card validation failed");
            }

            String network = detectNetwork(request.card.number);
            String last4 = request.card.number.substring(request.card.number.length() - 4);

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

        return badRequest("BAD_REQUEST_ERROR", "Invalid payment method");
    }

    private void processPayment(String paymentId, boolean upi) {

        boolean testMode = Boolean.parseBoolean(
                System.getenv().getOrDefault("TEST_MODE", "false")
        );

        boolean forcedSuccess = Boolean.parseBoolean(
                System.getenv().getOrDefault("TEST_PAYMENT_SUCCESS", "true")
        );

        int delay = Integer.parseInt(
                System.getenv().getOrDefault("TEST_PROCESSING_DELAY", "1000")
        );

        try {
            Thread.sleep(testMode ? delay : (upi ? 6000 : 8000));
        } catch (InterruptedException ignored) {}

        boolean success = testMode
                ? forcedSuccess
                : (upi ? random.nextInt(10) < 9 : random.nextInt(100) < 95);

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

    private boolean isValidVpa(String vpa) {
        return Pattern.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$", vpa);
    }

    private boolean isValidCard(Card card) {
        return card.number != null && luhn(card.number)
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
