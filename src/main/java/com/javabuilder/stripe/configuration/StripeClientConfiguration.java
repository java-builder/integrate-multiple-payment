package com.javabuilder.stripe.configuration;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeClientConfiguration {

    @Value("${stripe.api-key}")
    private String apiKey;

    @Bean
    public StripeClient stripeClient() {
        return StripeClient.builder()
                .setApiKey(apiKey)
                .build();
    }
}
