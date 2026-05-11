package com.autosalone.models.discounts;

import java.math.BigDecimal;

public class NoDiscountStrategy implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscountAmount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }
}
