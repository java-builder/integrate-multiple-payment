package com.javabuilder.stripe.configuration;

import com.paypal.sdk.Environment;
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.authentication.ClientCredentialsAuthModel;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaypalClientConfiguration {

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Bean
    PaypalServerSdkClient paypalServerSdkClient() {
        return new PaypalServerSdkClient.Builder()
                .loggingConfig(builder -> builder
                        .level(Level.INFO)
                        .requestConfig(logConfigBuilder -> logConfigBuilder.body(true))
                        .responseConfig(logConfigBuilder -> logConfigBuilder.body(true)))
                .httpClientConfig(configBuilder -> configBuilder.timeout(30))
                .clientCredentialsAuth(new ClientCredentialsAuthModel.Builder(
                        clientId,
                        clientSecret
                ).build())
                .environment(Environment.SANDBOX)
                .build();
    }
}
