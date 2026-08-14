package com.autosalone.services;

import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.CustomerListResponse;
import com.autosalone.dtos.CustomerRequest;
import com.autosalone.dtos.CustomerResponse;
import com.autosalone.enums.TokenType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Customer;
import com.autosalone.repositories.AuthTokenRepository;
import com.autosalone.repositories.CustomerRepository;
import com.autosalone.repositories.UserRepository;
import com.autosalone.utils.Utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CustomerService {

    @Inject
    private CustomerRepository customerRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private AuthTokenRepository authTokenRepository;

    // read

    public Customer getCustomerById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found of id: " + id));
    }

    private boolean hasActiveInvitation(Customer customer) {
        return authTokenRepository
                .findByUserAndType(customer, TokenType.REGISTRATION)
                .map(token -> !token.isExpired())
                .orElse(false);
    }

    public CustomerResponse getCustomerResponseById(UUID id) {
        Customer customer = getCustomerById(id);
        boolean hasActiveInvitation = hasActiveInvitation(customer);
        return CustomerResponse.fromEntity(customer, hasActiveInvitation);
    }

    public List<CustomerListResponse> getCustomers(String keyword, Boolean isActive) {
        String sanitizedKeyword = Utils.sanitizeLikeKeyword(keyword);
        return customerRepository.findCustomers(sanitizedKeyword, isActive).stream()
                .map(CustomerListResponse::fromEntity)
                .toList();
    }

    // write

    @Transactional
    public CustomerResponse addCustomer(CustomerRequest request) {

        if (request.email() != null && userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("This email is already in use");
        }

        Customer customer = new Customer.CustomerBuilder()
                .setFirstName(request.firstName())
                .setLastName(request.lastName())
                .setPhoneNumber(request.phoneNumber())
                .setEmail(request.email())
                .setResidenceCity(request.residenceCity())
                .setZipCode(request.zipCode())
                .setFiscalCode(request.fiscalCode())
                .setVatNumber(request.vatNumber())
                .build();

        customerRepository.save(customer);
        return CustomerResponse.fromEntity(customer, false);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, CustomerRequest request) {
        Customer customer = getCustomerById(id);
        boolean hasActiveInvitation = hasActiveInvitation(customer);

        if (request.email() == null && (customer.isActive() || customer.getPassword() != null)) {
            throw new IllegalStateException("Cannot remove the email, the user is active");
        }

        if (customer.getEmail() != null && request.email() != null && !request.email().equals(customer.getEmail())) {
            userRepository.findByEmail(request.email()).ifPresent(existing -> {
                throw new IllegalStateException("This email is already in use");
            });
        }

        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setPhoneNumber(request.phoneNumber());
        customer.setEmail(request.email());
        customer.setResidenceCity(request.residenceCity());
        customer.setZipCode(request.zipCode());
        customer.setFiscalCode(request.fiscalCode());
        customer.setVatNumber(request.vatNumber());

        customerRepository.save(customer);
        return CustomerResponse.fromEntity(customer, hasActiveInvitation);
    }
}
