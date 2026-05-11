package com.autosalone.models.discounts;

import java.math.BigDecimal;

public interface DiscountStrategy {
    BigDecimal calculateDiscountAmount(BigDecimal subtotal);
}
