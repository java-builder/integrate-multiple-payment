package com.javabuilder.stripe.service;

import com.javabuilder.stripe.common.PaymentStatus;
import com.javabuilder.stripe.dto.request.CheckoutRequest;
import com.javabuilder.stripe.dto.request.CreateLinkPaymentRequest;
import com.javabuilder.stripe.dto.response.CheckoutResponse;
import com.javabuilder.stripe.entity.Order;
import com.javabuilder.stripe.entity.Payment;
import com.javabuilder.stripe.repository.OrderRepository;
import com.javabuilder.stripe.repository.PaymentRepository;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-SERVICE")
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final StripePaymentService stripePaymentService;

    @Transactional(rollbackFor = Exception.class)
    public CheckoutResponse checkout(CheckoutRequest request) {
        Order order = orderRepository.findByOrderCode(request.orderCode())
                .orElseThrow(() -> new RuntimeException("Order not found with code: " + request.orderCode()));

        CreateLinkPaymentRequest stripeRequest = new CreateLinkPaymentRequest(order.getOrderCode());
        Session session = stripePaymentService.createCheckoutSession(stripeRequest);

        Payment payment = Payment.builder()
                .order(order)
                .paymentGateway(request.paymentGateway())
                .amount(order.getTotalPrice())
                .currency(order.getCurrency())
                .gatewayTransactionId(session.getId())
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);
        log.info("Payment transaction saved successfully. ID: {}, Stripe Session ID: {}", payment.getId(), session.getId());

        return CheckoutResponse.builder()
                .paymentUrl(session.getUrl())
                .gatewayTransactionId(session.getId())
                .build();
    }
}
