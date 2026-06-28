package com.autosalone.services;

import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.CustomerCreateRequest;
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
    public UUID addCustomer(CustomerCreateRequest request) {

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
}
