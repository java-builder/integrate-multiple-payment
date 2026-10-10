package com.javabuilder.stripe.strategy.vnpay;

import com.javabuilder.stripe.common.PaymentGateway;
import com.javabuilder.stripe.dto.response.PaymentGatewayResponse;
import com.javabuilder.stripe.strategy.PaymentStrategy;
import com.javabuilder.stripe.strategy.dto.PaymentGatewayCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "VNPAY-PAYMENT-STRATEGY")
public class VnPayPaymentStrategy implements PaymentStrategy {

    private final VnPayProperties vnPayProperties;

    @Override
    public PaymentGateway getPaymentGateway() {
        return PaymentGateway.VNPAY;
    }

    @Override
    public PaymentGatewayResponse processPayment(PaymentGatewayCommand command) {
        long amount = command.totalPrice().multiply(BigDecimal.valueOf(100)).longValue();
        String vnpIpAddr = command.clientIp();
        String vnpTxnRef = VnPayUtils.getRandomNumber(8);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayProperties.getVersion());
        vnpParams.put("vnp_Command", vnPayProperties.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        vnpParams.put("vnp_OrderType", vnPayProperties.getVnpOrderType());
        vnpParams.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        vnpParams.put("vnp_IpAddr", vnpIpAddr);
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", "Payment for orderCode " + command.orderCode());

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(calendar.getTime());

        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        calendar.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnpSecureHash  = VnPayUtils.hmacSHA512(vnPayProperties.getSecretKey(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        String paymentUrl = vnPayProperties.getVnpUrl() + "?" + queryUrl;

        return PaymentGatewayResponse.builder()
                .gatewayTransactionId(command.orderCode())
                .paymentUrl(paymentUrl)
                .build();
    }
}
