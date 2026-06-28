package com.autosalone.dtos;

public record CustomerUpdateRequest(
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        String residenceCity,
        String zipCode,
        String fiscalCode,
        String vatNumber) {
}
