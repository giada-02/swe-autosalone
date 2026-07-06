package com.autosalone.enums;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public enum ExpirationPolicy {
    TEN_DAYS {
        @Override
        public LocalDate calculateExpirationDate(LocalDate date, LocalDate customExpirationDate) {
            return date.plusDays(10);
        }
    },
    END_OF_MONTH {
        @Override
        public LocalDate calculateExpirationDate(LocalDate date, LocalDate customExpirationDate) {
            return date.with(TemporalAdjusters.lastDayOfMonth());
        }
    },
    CUSTOM {
        @Override
        public LocalDate calculateExpirationDate(LocalDate date, LocalDate customExpirationDate) {
            if (customExpirationDate == null) {
                throw new IllegalArgumentException(
                        "Expiration policy of type CUSTOM requires a custom expiration date");
            }
            if (customExpirationDate.isBefore(date))
                throw new IllegalArgumentException("Cannot set the expiration date before the document date");
            return customExpirationDate;
        }
    };

    public abstract LocalDate calculateExpirationDate(LocalDate date, LocalDate customExpirationDate);
}
