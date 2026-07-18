package com.javabuilder.stripe.controller;

import com.javabuilder.stripe.dto.request.CheckoutRequest;
import com.javabuilder.stripe.dto.response.ApiResponse;
import com.javabuilder.stripe.dto.response.CheckoutResponse;
import com.javabuilder.stripe.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    ApiResponse<CheckoutResponse> checkout(@RequestBody @Valid CheckoutRequest request) {
        var data = paymentService.checkout(request);
        return ApiResponse.<CheckoutResponse>builder()
                .status("success")
                .message("Stripe checkout session created successfully")
                .data(data)
                .build();
    }
}
