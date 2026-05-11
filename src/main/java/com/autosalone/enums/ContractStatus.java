package com.autosalone.enums;

public enum ContractStatus implements DocumentLifecycleStatus<ContractStatus> {
    DRAFT {

        @Override
        public boolean canTransitionTo(ContractStatus next) {
            return next == ContractStatus.CONFIRMED;
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
    CONFIRMED {

        @Override
        public boolean canTransitionTo(ContractStatus next) {
            return next == ContractStatus.COMPLETED || next == ContractStatus.CANCELLED;
        }

        @Override
        public boolean isArchivable() {
            return false;
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
    COMPLETED {

        @Override
        public boolean canTransitionTo(ContractStatus next) {
            return false;
        }

        @Override
        public boolean isArchivable() {
            return false;
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
    CANCELLED {

        @Override
        public boolean canTransitionTo(ContractStatus next) {
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
            return false;
        }
    };
}
