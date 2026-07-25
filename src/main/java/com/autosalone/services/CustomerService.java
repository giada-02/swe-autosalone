package com.autosalone.services;

import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.CustomerRequest;
import com.autosalone.models.Customer;
import com.autosalone.repositories.CustomerRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CustomerService {

    @Inject
    private CustomerRepository customerRepository;

    // read

    public Customer getCustomerById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found of id: " + id));
    }

    public List<Customer> getCustomers(String keyword, Boolean isActive) {
        return customerRepository.findCustomers(keyword, isActive);
    }

    // write

    @Transactional
    public UUID addCustomer(CustomerRequest request) {

        if (customerRepository.findByEmail(request.email()).isPresent()) {
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
        return customer.getId();
    }

    @Transactional
    public void updateCustomer(UUID customerId, CustomerRequest request) {
        Customer customer = getCustomerById(customerId);

        if (!customer.getEmail().equals(request.email())) {
            customerRepository.findByEmail(request.email()).ifPresent(existing -> {
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
    }

}
