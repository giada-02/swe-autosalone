package com.autosalone.dtos;

import java.util.UUID;

import com.autosalone.models.Customer;

public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        boolean isActive,
        String residenceCity,
        String zipCode,
        String fiscalCode,
        String vatNumber) {

    public static CustomerResponse fromEntity(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                customer.isActive(),
                customer.getResidenceCity(),
                customer.getZipCode(),
                customer.getFiscalCode(),
                customer.getVatNumber());
    }
}