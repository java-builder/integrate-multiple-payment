package com.javabuilder.stripe.strategy.stripe;

import com.javabuilder.stripe.common.Currency;
import com.javabuilder.stripe.common.PaymentGateway;
import com.javabuilder.stripe.dto.response.PaymentGatewayResponse;
import com.javabuilder.stripe.exception.AppException;
import com.javabuilder.stripe.exception.ErrorCode;
import com.javabuilder.stripe.strategy.PaymentStrategy;
import com.javabuilder.stripe.strategy.dto.PaymentGatewayCommand;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "STRIPE-PAYMENT-STRATEGY")
public class StripePaymentStrategy implements PaymentStrategy {

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    private final StripeClient stripeClient;

    @Override
    public PaymentGateway getPaymentGateway() {
        return PaymentGateway.STRIPE;
    }

    @Override
    public PaymentGatewayResponse processPayment(PaymentGatewayCommand command) {
        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(command.orderCode())
                .putMetadata("orderCode", command.orderCode())
                .setMode(SessionCreateParams.Mode.PAYMENT);

        for (PaymentGatewayCommand.LineItem lineItem : command.lineItems()) {
            Long itemAmount = toStripeAmount(lineItem.unitPrice(), command.currency());

            sessionBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(lineItem.quantity().longValue())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(command.currency().toStripeCode())
                            .setUnitAmount(itemAmount)
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(lineItem.name())
                                    .build())
                            .build())
                    .build());
        }

        SessionCreateParams params = sessionBuilder.build();
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("checkout-" + command.orderCode())
                .build();

        try {
            Session session = stripeClient.v1().checkout().sessions().create(params, options);
            return PaymentGatewayResponse.builder()
                    .gatewayTransactionId(session.getId())
                    .paymentUrl(session.getUrl())
                    .build();
        } catch (StripeException e) {
            log.error("Stripe Checkout Session creation failed", e);
            throw new AppException(ErrorCode.STRIPE_INTEGRATION_FAILED);
        }
    }

    private Long toStripeAmount(BigDecimal amount, Currency currency) {
        if (amount == null) {
            return 0L;
        }
        return currency.isZeroDecimal() ?
                amount.setScale(0, RoundingMode.HALF_UP).longValue() :
                amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
