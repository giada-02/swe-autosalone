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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
