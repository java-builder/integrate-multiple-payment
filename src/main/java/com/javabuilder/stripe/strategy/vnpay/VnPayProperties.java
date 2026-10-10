package com.javabuilder.stripe.strategy.vnpay;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VnPayProperties {
    private String tmnCode;
    private String secretKey;
    private String vnpUrl;
    private String returnUrl;
    private String version = "2.1.0";
    private String vnpOrderType = "other";
    private String command = "pay";
}
