package com.autosalone.models.discounts;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageDiscountStrategy implements DiscountStrategy {
    private final BigDecimal percentageValue;
    private final BigDecimal calculationMultiplier;

    public PercentageDiscountStrategy(BigDecimal percentageValue) {
        validatePercentageValue(percentageValue);
        this.percentageValue = percentageValue;
        this.calculationMultiplier = percentageValue.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal getPercentageValue() {
        return percentageValue;
    }

    @Override
    public BigDecimal calculateDiscountAmount(BigDecimal subtotal) {
        return subtotal.multiply(this.calculationMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private void validatePercentageValue(BigDecimal percentageValue) {
        if (percentageValue == null || percentageValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The percentage must be > 0");
        }
    }

}
