package com.gateway.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder {

    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void seedMerchant() {
        String existsSql = "SELECT COUNT(*) FROM merchants WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(
                existsSql,
                Integer.class,
                "test@example.com"
        );

        if (count != null && count == 0) {
            jdbcTemplate.update("""
                INSERT INTO merchants 
                (id, name, email, api_key, api_secret, created_at, updated_at)
                VALUES 
                ('550e8400-e29b-41d4-a716-446655440000',
                 'Test Merchant',
                 'test@example.com',
                 'key_test_abc123',
                 'secret_test_xyz789',
                 CURRENT_TIMESTAMP,
                 CURRENT_TIMESTAMP)
            """);
        }
    }
}
