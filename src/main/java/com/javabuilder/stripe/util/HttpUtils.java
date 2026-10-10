package com.javabuilder.stripe.util;

import jakarta.servlet.http.HttpServletRequest;

public final class HttpUtils {

    private HttpUtils() {
    }

    public static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
