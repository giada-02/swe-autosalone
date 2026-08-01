package com.autosalone.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.CustomerRequest;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Customer;
import com.autosalone.repositories.CustomerRepository;
import com.autosalone.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private UUID customerId;
    private Customer mockCustomer;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        mockCustomer = mock(Customer.class);

        customerRequest = new CustomerRequest(
                "Mario",
                "Rossi",
                "3331234567",
                "mario.rossi@example.com",
                "Roma",
                "00100",
                "RSSMRA80A01H501Z",
                null);
    }

    // read

    @Test
    void getCustomerById_Success() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        Customer result = customerService.getCustomerById(customerId);
        assertNotNull(result);
        assertEquals(mockCustomer, result);
    }

    @Test
    void getCustomerById_NotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            customerService.getCustomerById(customerId);
        });
    }

    @Test
    void getCustomers_Success() {
        when(customerRepository.findCustomers("Mario", true)).thenReturn(List.of(mockCustomer));

        List<Customer> results = customerService.getCustomers("Mario", true);

        assertEquals(1, results.size());
        verify(customerRepository).findCustomers("Mario", true);
    }

    // write

    @Test
    void addCustomer_Success() {
        when(userRepository.findByEmail(customerRequest.email())).thenReturn(Optional.empty());

        customerService.addCustomer(customerRequest);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());

        Customer savedCustomer = captor.getValue();
        assertEquals("Mario", savedCustomer.getFirstName());
        assertEquals("Rossi", savedCustomer.getLastName());
        assertEquals("mario.rossi@example.com", savedCustomer.getEmail());
    }

    @Test
    void addCustomer_FailsIfEmailAlreadyInUse() {
        when(userRepository.findByEmail(customerRequest.email())).thenReturn(Optional.of(mockCustomer));

        assertThrows(IllegalStateException.class, () -> {
            customerService.addCustomer(customerRequest);
        });

        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateCustomer_Success_SameEmail() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(mockCustomer.getEmail()).thenReturn(customerRequest.email());

        assertDoesNotThrow(() -> customerService.updateCustomer(customerId, customerRequest));

        verify(userRepository, never()).findByEmail(anyString());

        verify(mockCustomer).setFirstName("Mario");
        verify(mockCustomer).setLastName("Rossi");
        verify(customerRepository).save(mockCustomer);
    }

    @Test
    void updateCustomer_Success_DifferentEmail() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(mockCustomer.getEmail()).thenReturn("vecchia.email@example.com");

        when(userRepository.findByEmail(customerRequest.email())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> customerService.updateCustomer(customerId, customerRequest));

        verify(userRepository).findByEmail(customerRequest.email());

        verify(mockCustomer).setEmail(customerRequest.email());
        verify(customerRepository).save(mockCustomer);
    }

    @Test
    void updateCustomer_FailsIfDifferentEmailAlreadyInUse() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(mockCustomer.getEmail()).thenReturn("vecchia.email@example.com");

        Customer anotherCustomer = mock(Customer.class);
        when(userRepository.findByEmail(customerRequest.email())).thenReturn(Optional.of(anotherCustomer));

        assertThrows(IllegalStateException.class, () -> {
            customerService.updateCustomer(customerId, customerRequest);
        });

        verify(customerRepository, never()).save(any());
    }
}