package com.gateway.config;

import com.gateway.models.Merchant;
import com.gateway.repositories.MerchantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedMerchant(MerchantRepository merchantRepository) {
        return args -> {

            String testEmail = "test@example.com";

            if (merchantRepository.findByEmail(testEmail).isPresent()) {
                return;
            }

            Merchant merchant = new Merchant();
            merchant.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            merchant.setName("Test Merchant");
            merchant.setEmail(testEmail);
            merchant.setApiKey("key_test_abc123");
            merchant.setApiSecret("secret_test_xyz789");

            merchantRepository.save(merchant);
        };
    }
}
