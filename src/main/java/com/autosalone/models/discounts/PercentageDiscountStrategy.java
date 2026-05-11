package com.autosalone.models.discounts;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageDiscountStrategy implements DiscountStrategy {
    private final BigDecimal percentage;

    public PercentageDiscountStrategy(double percentageValue) {
        this.percentage = BigDecimal.valueOf(percentageValue).divide(BigDecimal.valueOf(100));
    }
    
    @Override
    public BigDecimal calculateDiscountAmount(BigDecimal subtotal) {
        return subtotal.multiply(this.percentage).setScale(2, RoundingMode.HALF_UP);
    }

}
