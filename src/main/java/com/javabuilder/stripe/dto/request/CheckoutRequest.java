package com.javabuilder.stripe.dto.request;

import com.javabuilder.stripe.common.PaymentGateway;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotBlank(message = "Order code cannot be blank")
        String orderCode,

        @NotNull(message = "Payment gateway cannot be null")
        PaymentGateway paymentGateway
) {}
