package com.javabuilder.stripe.strategy.vnpay;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Slf4j(topic = "VNPAY-UTILS")
public final class VnPayUtils {

    private VnPayUtils(){
    }

    private static final String ALGORITHM = "HmacSHA512";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hmacSHA512(final String key, final String data) {
        try {
            if(StringUtils.isBlank(key) || StringUtils.isBlank(data)) {
                throw new NullPointerException("Key or data is null");
            }
            final Mac hmac512 = Mac.getInstance(ALGORITHM);
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, ALGORITHM);
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        }catch (Exception e){
            log.error("Error occurred while generating HMAC SHA512", e);
            return "";
        }
    }

    public static String getRandomNumber(int length) {
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

}
