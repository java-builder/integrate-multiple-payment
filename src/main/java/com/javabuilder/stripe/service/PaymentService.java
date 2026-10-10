package com.javabuilder.stripe.service;

import com.javabuilder.stripe.common.OrderStatus;
import com.javabuilder.stripe.common.PaymentStatus;
import com.javabuilder.stripe.dto.request.PaymentInitRequest;
import com.javabuilder.stripe.dto.response.PaymentGatewayResponse;
import com.javabuilder.stripe.dto.response.PaymentInitResponse;
import com.javabuilder.stripe.entity.Order;
import com.javabuilder.stripe.entity.Payment;
import com.javabuilder.stripe.exception.AppException;
import com.javabuilder.stripe.exception.ErrorCode;
import com.javabuilder.stripe.repository.OrderRepository;
import com.javabuilder.stripe.repository.PaymentRepository;
import com.javabuilder.stripe.strategy.PaymentStrategy;
import com.javabuilder.stripe.strategy.PaymentStrategyFactory;
import com.javabuilder.stripe.strategy.dto.PaymentGatewayCommand;
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
    private final PaymentStrategyFactory paymentStrategyFactory;

    @Transactional(rollbackFor = Exception.class)
    public PaymentInitResponse initiatePayment(PaymentInitRequest request, String clientIp) {
        Order order = orderRepository.findByOrderCode(request.orderCode())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getOrderStatus() == OrderStatus.SUCCESS) {
            throw new IllegalStateException("This order has already been paid!");
        }

        PaymentGatewayCommand command = PaymentGatewayCommand.builder()
                .orderCode(order.getOrderCode())
                .totalPrice(order.getTotalPrice())
                .currency(order.getCurrency())
                .clientIp(clientIp)
                .lineItems(order.getOrderDetails().stream().map(orderDetail -> PaymentGatewayCommand.LineItem.builder()
                        .name(orderDetail.getProduct().getName())
                        .quantity(orderDetail.getQuantity())
                        .unitPrice(orderDetail.getPrice())
                        .build())
                        .toList())
                .build();

        PaymentStrategy paymentStrategy = paymentStrategyFactory.getPaymentStrategy(request.paymentGateway());
        PaymentGatewayResponse paymentGatewayResponse = paymentStrategy.processPayment(command);

        Payment payment = Payment.builder()
                .order(order)
                .paymentGateway(request.paymentGateway())
                .amount(order.getTotalPrice())
                .currency(order.getCurrency())
                .gatewayTransactionId(paymentGatewayResponse.gatewayTransactionId())
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);
        log.info("Payment transaction saved successfully. ID: {}, Stripe Session ID: {}", payment.getId(), paymentGatewayResponse.gatewayTransactionId());

        return PaymentInitResponse.builder()
                .paymentUrl(paymentGatewayResponse.paymentUrl())
                .gatewayTransactionId(paymentGatewayResponse.gatewayTransactionId())
                .build();
    }
}
