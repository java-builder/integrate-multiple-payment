package com.javabuilder.stripe.common;

public enum Currency {
    VND,
    USD,

    ;
    public String toStripeCode() {
        return name().toLowerCase();
    }

    public boolean isZeroDecimal() {
        return this == VND;
    }
}
