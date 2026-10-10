package com.javabuilder.stripe.dto.response;

import com.javabuilder.stripe.common.Currency;
import com.javabuilder.stripe.common.OrderStatus;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record OrderResponse(
        Long id,
        String orderCode,
        OrderStatus orderStatus,
        BigDecimal totalPrice,
        Currency currency,
        List<OrderItemResponse> orderItems,
        Instant createdAt
) {}
