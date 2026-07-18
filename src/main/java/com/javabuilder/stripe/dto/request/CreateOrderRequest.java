package com.javabuilder.stripe.dto.request;

import com.javabuilder.stripe.common.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "Order items cannot be empty")
        @Valid
        List<OrderItemRequest> items,

        @NotNull(message = "Currency cannot be null")
        Currency currency
) {}
