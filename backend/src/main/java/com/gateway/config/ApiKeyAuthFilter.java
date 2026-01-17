package com.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.models.Merchant;
import com.gateway.repositories.MerchantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final MerchantRepository merchantRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKeyAuthFilter(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    /**
     * Decide which endpoints DO NOT require API key authentication
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.equals("/health")
                || path.contains("/public")
                || path.startsWith("/api/v1/checkout");
    }

    /**
     * API key authentication for protected endpoints
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String apiKey = request.getHeader("X-Api-Key");
        String apiSecret = request.getHeader("X-Api-Secret");

        // Missing headers
        if (apiKey == null || apiSecret == null) {
            sendAuthError(response);
            return;
        }

        Optional<Merchant> merchantOpt = merchantRepository.findByApiKey(apiKey);

        // Invalid / inactive merchant
        if (merchantOpt.isEmpty()
                || !merchantOpt.get().getApiSecret().equals(apiSecret)
                || !merchantOpt.get().isActive()) {
            sendAuthError(response);
            return;
        }

        // Attach merchant for downstream controllers
        request.setAttribute("merchant", merchantOpt.get());

        filterChain.doFilter(request, response);
    }

    /**
     * Standard authentication error response
     */
    private void sendAuthError(HttpServletResponse response) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String body = objectMapper.writeValueAsString(
                new ErrorResponse(
                        new ErrorDetail(
                                "AUTHENTICATION_ERROR",
                                "Invalid API credentials"
                        )
                )
        );

        response.getWriter().write(body);
        response.getWriter().flush();
    }

    // ===== Response DTOs =====
    record ErrorResponse(ErrorDetail error) {}
    record ErrorDetail(String code, String description) {}
}
