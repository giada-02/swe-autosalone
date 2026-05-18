package com.autosalone.models.discounts;

import java.math.BigDecimal;

public class FixedAmountDiscountStrategy implements DiscountStrategy {
    private final BigDecimal discountAmount;

    public FixedAmountDiscountStrategy(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    @Override
    public BigDecimal calculateDiscountAmount(BigDecimal subtotal) {
        if (discountAmount.compareTo(subtotal) > 0) { // the discount is higher than the subtotal
            return subtotal;
        }
        return discountAmount;
    }
}
