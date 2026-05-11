package com.autosalone.enums;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public enum ExpirationPolicy {
    TEN_DAYS {
        @Override
        public LocalDate calculateExpirationDate(LocalDate date) {
            return date.plusDays(10);
        }
    },
    END_OF_MONTH {
        @Override
        public LocalDate calculateExpirationDate(LocalDate date) {
            return date.with(TemporalAdjusters.lastDayOfMonth());
        }
    },
    CUSTOM {
        @Override
        public LocalDate calculateExpirationDate(LocalDate date) {
            return null;
        }
    };

    public abstract LocalDate calculateExpirationDate(LocalDate date);
}
