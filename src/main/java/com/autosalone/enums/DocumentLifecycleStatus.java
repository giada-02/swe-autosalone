package com.autosalone.enums;

/**
 * Defines the shared lifecycle rules for any sales document status.
 * 
 * @param <T> The specific enum type (ContractStatus or QuotationStatus)
 */
public interface DocumentLifecycleStatus<T extends Enum<T>> {

    boolean canTransitionTo(T next);

    boolean isArchivable();

    boolean isEditable();

    boolean canBeVisibleToCustomer();

}