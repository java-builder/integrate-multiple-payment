package com.javabuilder.stripe.strategy;

import com.javabuilder.stripe.common.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "PAYMENT-STRATEGY-FACTORY")
public class PaymentStrategyFactory {

    private final Map<PaymentGateway, PaymentStrategy> paymentStrategyMap;

    public PaymentStrategyFactory(List<PaymentStrategy> strategies) {
        this.paymentStrategyMap = strategies.stream()
                .collect(Collectors.toMap(PaymentStrategy::getPaymentGateway, Function.identity()));
        log.info("PaymentStrategyFactory loaded successfully with gateways: {}", paymentStrategyMap.keySet());
    }

    public PaymentStrategy getPaymentStrategy(PaymentGateway paymentGateway) {
        return Optional.ofNullable(paymentStrategyMap.get(paymentGateway))
                .orElseThrow(() -> new IllegalArgumentException("Payment strategy not found for gateway: " + paymentGateway));
    }
}
