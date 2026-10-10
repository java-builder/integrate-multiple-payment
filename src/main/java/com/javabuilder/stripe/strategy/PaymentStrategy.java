package com.javabuilder.stripe.strategy;

import com.javabuilder.stripe.common.PaymentGateway;
import com.javabuilder.stripe.dto.response.PaymentGatewayResponse;
import com.javabuilder.stripe.strategy.dto.PaymentGatewayCommand;

public interface PaymentStrategy {
    PaymentGateway getPaymentGateway();
    PaymentGatewayResponse processPayment(PaymentGatewayCommand command);
}
