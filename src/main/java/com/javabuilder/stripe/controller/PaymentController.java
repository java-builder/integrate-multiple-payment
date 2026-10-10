package com.javabuilder.stripe.controller;

import com.javabuilder.stripe.dto.request.PaymentInitRequest;
import com.javabuilder.stripe.dto.response.ApiResponse;
import com.javabuilder.stripe.dto.response.PaymentInitResponse;
import com.javabuilder.stripe.service.PaymentService;
import com.javabuilder.stripe.util.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<PaymentInitResponse> initiatePayment(@RequestBody @Valid PaymentInitRequest request, HttpServletRequest httpServletRequest) {
        String clientIp = HttpUtils.getClientIpAddress(httpServletRequest);
        var data = paymentService.initiatePayment(request, clientIp);
        return ApiResponse.<PaymentInitResponse>builder()
                .status("success")
                .message("Payment initiated successfully")
                .data(data)
                .build();
    }
}
