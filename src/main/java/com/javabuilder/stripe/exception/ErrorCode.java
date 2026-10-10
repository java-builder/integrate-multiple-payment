package com.javabuilder.stripe.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    PRODUCT_NOT_FOUND(404, "Product not found", HttpStatus.NOT_FOUND),
    OUT_OF_STOCK(400, "Product is out of stock", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(404, "Order not found", HttpStatus.NOT_FOUND),
    STRIPE_INTEGRATION_FAILED(500, "Stripe integration failed", HttpStatus.INTERNAL_SERVER_ERROR),
    PAYMENT_AMOUNT_MISMATCH(400, "Payment amount mismatch", HttpStatus.BAD_REQUEST),
    PAYPAL_INTEGRATION_FAILED(500, "PayPal integration failed", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
