package com.autosalone.models.discounts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.autosalone.enums.DiscountType;

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
            throw new IllegalArgumentException("The percentage cannot be null or negative");
        }
    }

    @Override
    public DiscountType getType() {
        return DiscountType.PERCENTAGE;
    }

    @Override
    public BigDecimal getValue() {
        return this.percentageValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PercentageDiscountStrategy that = (PercentageDiscountStrategy) o;

        if (this.percentageValue == null && that.percentageValue == null)
            return true;
        if (this.percentageValue == null || that.percentageValue == null)
            return false;

        return this.percentageValue.compareTo(that.percentageValue) == 0;
    }

    @Override
    public int hashCode() {
        return percentageValue != null ? percentageValue.stripTrailingZeros().hashCode() : 0;
    }
}
