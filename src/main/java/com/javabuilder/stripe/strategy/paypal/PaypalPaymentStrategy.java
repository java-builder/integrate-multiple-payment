package com.javabuilder.stripe.strategy.paypal;

import com.javabuilder.stripe.common.Currency;
import com.javabuilder.stripe.common.PaymentGateway;
import com.javabuilder.stripe.dto.response.PaymentGatewayResponse;
import com.javabuilder.stripe.exception.AppException;
import com.javabuilder.stripe.exception.ErrorCode;
import com.javabuilder.stripe.strategy.PaymentStrategy;
import com.javabuilder.stripe.strategy.dto.PaymentGatewayCommand;
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.exceptions.ApiException;
import com.paypal.sdk.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PAYPAL-PAYMENT-STRATEGY")
public class PaypalPaymentStrategy implements PaymentStrategy {

    @Value("${paypal.return-url}")
    private String returnUrl;

    @Value("${paypal.cancel-url}")
    private String cancelUrl;

    private final PaypalServerSdkClient paypalServerSdkClient;

    @Override
    public PaymentGateway getPaymentGateway() {
        return PaymentGateway.PAYPAL;
    }

    @Override
    public PaymentGatewayResponse processPayment(PaymentGatewayCommand command) {
        Currency currency = command.currency();
        String code = currency.name();

        List<ItemRequest> items = new ArrayList<>();
        BigDecimal itemTotal = BigDecimal.ZERO;

        for (PaymentGatewayCommand.LineItem itemLine : command.lineItems()) {
            BigDecimal unitPrice = itemLine.unitPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemLine.quantity())).setScale(2, RoundingMode.HALF_UP);
            itemTotal = itemTotal.add(lineTotal);

            ItemRequest item = new ItemRequest.Builder(
                    itemLine.name(),
                    new Money(code, unitPrice.toPlainString()),
                    itemLine.quantity().toString()
            ).build();
            items.add(item);
        }

        String itemTotalValue = itemTotal.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String orderAmountValue = command.totalPrice().setScale(2, RoundingMode.HALF_UP).toPlainString();

        if (!itemTotalValue.equals(orderAmountValue)) {
            log.error("Payment amount mismatch: calculated itemTotal={}, command totalPrice={}", itemTotalValue, orderAmountValue);
            throw new AppException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest.Builder()
                .amount(new AmountWithBreakdown.Builder()
                        .value(itemTotalValue)
                        .currencyCode(code)
                        .breakdown(new AmountBreakdown.Builder()
                                .itemTotal(new Money.Builder()
                                        .value(itemTotalValue)
                                        .currencyCode(code)
                                        .build())
                                .build())
                        .build())
                .items(items)
                .customId(command.orderCode())
                .build();

        OrderRequest orderRequest = new OrderRequest.Builder()
                .intent(CheckoutPaymentIntent.CAPTURE)
                .purchaseUnits(List.of(purchaseUnitRequest))
                .paymentSource(new PaymentSource.Builder()
                        .paypal(new PaypalWallet.Builder()
                                .experienceContext(new PaypalWalletExperienceContext.Builder()
                                        .brandName("JavaBuilder")
                                        .shippingPreference(PaypalWalletContextShippingPreference.NO_SHIPPING)
                                        .returnUrl(returnUrl)
                                        .cancelUrl(cancelUrl)
                                        .userAction(PaypalExperienceUserAction.PAY_NOW)
                                        .build())
                                .build())
                        .build())
                .build();

        CreateOrderInput createOrderInput = new CreateOrderInput.Builder()
                .body(orderRequest)
                .paypalRequestId("checkout-" + command.orderCode())
                .build();

        try {
            Order order = paypalServerSdkClient.getOrdersController()
                    .createOrder(createOrderInput)
                    .getResult();

            String approveUrl = order.getLinks().stream()
                    .filter(l -> "payer-action".equals(l.getRel()))
                    .map(LinkDescription::getHref)
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.PAYPAL_INTEGRATION_FAILED));

            return PaymentGatewayResponse.builder()
                    .gatewayTransactionId(order.getId())
                    .paymentUrl(approveUrl)
                    .build();

        } catch (IOException | ApiException e) {
            log.error("PayPal order creation failed", e);
            throw new AppException(ErrorCode.PAYPAL_INTEGRATION_FAILED);
        }
    }

}
