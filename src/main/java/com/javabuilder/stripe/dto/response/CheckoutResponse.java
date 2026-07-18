package com.javabuilder.stripe.dto.response;

import lombok.Builder;

@Builder
public record CheckoutResponse(
        String paymentUrl,
        String gatewayTransactionId
) {}
