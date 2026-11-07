package com.autosalone.models.discounts;

import java.math.BigDecimal;

import com.autosalone.enums.DiscountType;

public class FixedAmountDiscountStrategy implements DiscountStrategy {
    private final BigDecimal discountAmount;

    public FixedAmountDiscountStrategy(BigDecimal discountAmount) {
        validateDiscountValue(discountAmount);
        this.discountAmount = discountAmount;
    }

    @Override
    public BigDecimal calculateDiscountAmount(BigDecimal subtotal) {
        if (discountAmount.compareTo(subtotal) > 0) { // the discount is higher than the subtotal
            return subtotal;
        }
        return discountAmount;
    }

    private void validateDiscountValue(BigDecimal discountAmount) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The discount value cannot be null or negative");
        }
    }

    @Override
    public DiscountType getType() {
        return DiscountType.FIXED;
    }

    @Override
    public BigDecimal getValue() {
        return this.discountAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FixedAmountDiscountStrategy that = (FixedAmountDiscountStrategy) o;

        return this.discountAmount.compareTo(that.discountAmount) == 0;
    }

    @Override
    public int hashCode() {
        return discountAmount.stripTrailingZeros().hashCode();
    }
}
