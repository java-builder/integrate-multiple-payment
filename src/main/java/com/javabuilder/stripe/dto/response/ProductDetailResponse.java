package com.javabuilder.stripe.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record ProductDetailResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        String imageUrl,
        Instant createdAt
) {
}
