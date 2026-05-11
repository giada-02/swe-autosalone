package com.autosalone.enums;

public enum QuotationStatus implements DocumentLifecycleStatus<QuotationStatus> {
    DRAFT {
        @Override
        public boolean canTransitionTo(QuotationStatus next) {
            return next == ISSUED;
        }

        @Override
        public boolean isArchivable() {
            return true;
        }

        @Override
        public boolean isEditable() {
            return true;
        }

        @Override
        public boolean canBeVisibleToCustomer() {
            return false;
        }
    },
    ISSUED {
        @Override
        public boolean canTransitionTo(QuotationStatus next) {
            return next == EXPIRED;
        }

        @Override
        public boolean isArchivable() {
            return true;
        }

        @Override
        public boolean isEditable() {
            return false;
        }

        @Override
        public boolean canBeVisibleToCustomer() {
            return true;
        }
    },
    EXPIRED {
        @Override
        public boolean canTransitionTo(QuotationStatus next) {
            return false;
        }

        @Override
        public boolean isArchivable() {
            return true;
        }

        @Override
        public boolean isEditable() {
            return false;
        }

        @Override
        public boolean canBeVisibleToCustomer() {
            return true;
        }
    };
}