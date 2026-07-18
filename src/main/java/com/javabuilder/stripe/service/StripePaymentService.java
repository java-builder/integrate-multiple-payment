package com.javabuilder.stripe.service;

import com.javabuilder.stripe.common.Currency;
import com.javabuilder.stripe.dto.request.CreateLinkPaymentRequest;
import com.javabuilder.stripe.entity.Order;
import com.javabuilder.stripe.entity.OrderDetail;
import com.javabuilder.stripe.entity.Product;
import com.javabuilder.stripe.repository.OrderRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "STRIPE-SERVICE")
public class StripePaymentService {

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    private final StripeClient stripeClient;
    private final OrderRepository orderRepository;

    @Transactional(rollbackFor = Exception.class)
    public Session createCheckoutSession(CreateLinkPaymentRequest request) {
        Order order = orderRepository.findOrderWithOrderDetailsByOrderCode(request.orderCode())
                .orElseThrow(() -> new RuntimeException("Order not found with code: " + request.orderCode()));

        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(order.getOrderCode())
                .putMetadata("orderCode", order.getOrderCode())
                .setMode(SessionCreateParams.Mode.PAYMENT);

        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            Long itemAmount = toStripeAmount(detail.getPrice(), order.getCurrency());

            sessionBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(detail.getQuantity().longValue())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(order.getCurrency().toStripeCode())
                            .setUnitAmount(itemAmount)
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(product.getName())
                                    .build())
                            .build())
                    .build());
        }

        SessionCreateParams params = sessionBuilder.build();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("checkout-" + order.getOrderCode())
                .build();

        try {
            return stripeClient.v1().checkout().sessions().create(params, options);
        } catch (StripeException e) {
            log.error("Stripe Checkout Session creation failed", e);
            throw new RuntimeException("Stripe integration failed", e);
        }
    }

    private Long toStripeAmount(BigDecimal amount, Currency currency) {
        return currency.isZeroDecimal() ?
                amount.longValueExact() :
                amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }
}
