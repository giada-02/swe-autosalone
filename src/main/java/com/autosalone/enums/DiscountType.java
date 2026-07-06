package com.autosalone.enums;

import java.math.BigDecimal;
import com.autosalone.models.discounts.DiscountStrategy;
import com.autosalone.models.discounts.FixedAmountDiscountStrategy;
import com.autosalone.models.discounts.NoDiscountStrategy;
import com.autosalone.models.discounts.PercentageDiscountStrategy;

public enum DiscountType {

    FIXED {
        @Override
        public DiscountStrategy createStrategy(BigDecimal value) {
            return new FixedAmountDiscountStrategy(value);
        }
    },
    PERCENTAGE {
        @Override
        public DiscountStrategy createStrategy(BigDecimal value) {
            return new PercentageDiscountStrategy(value);
        }
    },
    NONE {
        @Override
        public DiscountStrategy createStrategy(BigDecimal value) {
            return new NoDiscountStrategy();
        }
    };

    public abstract DiscountStrategy createStrategy(BigDecimal value);
}