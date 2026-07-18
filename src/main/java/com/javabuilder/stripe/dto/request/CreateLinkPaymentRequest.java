package com.javabuilder.stripe.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateLinkPaymentRequest(
        @NotBlank(message = "Order code cannot be blank")
        String orderCode
) {}
