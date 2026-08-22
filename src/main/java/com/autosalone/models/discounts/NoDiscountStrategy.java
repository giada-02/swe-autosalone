package com.autosalone.models.discounts;

import java.math.BigDecimal;

import com.autosalone.enums.DiscountType;

public class NoDiscountStrategy implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscountAmount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public DiscountType getType() {
        return DiscountType.NONE;
    }

    @Override
    public BigDecimal getValue() {
        return null;
    }
}
