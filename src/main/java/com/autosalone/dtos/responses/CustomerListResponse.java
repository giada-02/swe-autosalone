package com.autosalone.dtos.responses;

import java.util.UUID;

import com.autosalone.models.Customer;

public record CustomerListResponse(
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

    public static CustomerListResponse fromEntity(Customer customer) {
        return new CustomerListResponse(
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