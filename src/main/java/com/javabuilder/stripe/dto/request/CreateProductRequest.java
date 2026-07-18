package com.javabuilder.stripe.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank(message = "Product name cannot be blank")
        String name,

        @NotBlank(message = "Product description cannot be blank")
        String description,

        @DecimalMin(value = "0")
        BigDecimal price,

        @DecimalMin(value = "1")
        Integer quantity,

        String imageUrl
) {
}
