package com.gateway.util;

import java.time.YearMonth;
import java.util.regex.Pattern;

public class PaymentValidationUtil {

    // VPA pattern: username@bank
    private static final Pattern VPA_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$");

    /* =======================
       UPI VALIDATION
       ======================= */
    public static boolean isValidVpa(String vpa) {
        if (vpa == null || vpa.isBlank()) {
            return false;
        }
        return VPA_PATTERN.matcher(vpa).matches();
    }

    /* =======================
       CARD VALIDATION
       ======================= */

    // Luhn Algorithm
    public static boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null) return false;

        String cleaned = cardNumber.replaceAll("[\\s-]", "");
        if (!cleaned.matches("\\d{13,19}")) return false;

        int sum = 0;
        boolean alternate = false;

        for (int i = cleaned.length() - 1; i >= 0; i--) {
            int n = cleaned.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    // Card network detection
    public static String detectCardNetwork(String cardNumber) {
        String cleaned = cardNumber.replaceAll("[\\s-]", "");

        if (cleaned.startsWith("4")) return "visa";

        if (cleaned.matches("^5[1-5].*")) return "mastercard";

        if (cleaned.startsWith("34") || cleaned.startsWith("37")) return "amex";

        if (cleaned.startsWith("60") ||
            cleaned.startsWith("65") ||
            cleaned.matches("^8[1-9].*")) return "rupay";

        return "unknown";
    }

    // Expiry validation
    public static boolean isValidExpiry(String month, String year) {
        try {
            int mm = Integer.parseInt(month);
            int yy = Integer.parseInt(year);

            if (mm < 1 || mm > 12) return false;

            if (year.length() == 2) {
                yy += 2000;
            }

            YearMonth expiry = YearMonth.of(yy, mm);
            YearMonth now = YearMonth.now();

            return !expiry.isBefore(now);
        } catch (Exception e) {
            return false;
        }
    }
}
