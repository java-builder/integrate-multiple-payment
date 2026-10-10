package com.javabuilder.stripe.dto.response;

import lombok.Builder;

@Builder
public record PaymentInitResponse(
        String paymentUrl,
        String gatewayTransactionId
) {
}
