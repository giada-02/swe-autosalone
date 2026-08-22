package com.autosalone.models.discounts;

import java.math.BigDecimal;

import com.autosalone.enums.DiscountType;

public interface DiscountStrategy {
    BigDecimal calculateDiscountAmount(BigDecimal subtotal);

    DiscountType getType();

    BigDecimal getValue();
}
