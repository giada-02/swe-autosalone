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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.requests.CustomerRequest;
import com.autosalone.dtos.responses.CustomerListResponse;
import com.autosalone.dtos.responses.CustomerResponse;
import com.autosalone.enums.TokenType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.AuthToken;
import com.autosalone.models.Customer;
import com.autosalone.repositories.AuthTokenRepository;
import com.autosalone.repositories.CustomerRepository;
import com.autosalone.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

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
        Customer response = customerService.getCustomerById(customerId);
        assertNotNull(response);
        assertEquals(mockCustomer, response);
    }

    @Test
    void getCustomerById_NotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            customerService.getCustomerById(customerId);
        });
    }

    @Test
    void getCustomerResponseById_Success() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(authTokenRepository.findByUserAndType(mockCustomer, TokenType.REGISTRATION)).thenReturn(Optional.empty());
        when(mockCustomer.getId()).thenReturn(customerId);

        CustomerResponse response = customerService.getCustomerResponseById(customerId);

        assertNotNull(response);
        assertEquals(customerId, response.id());
        assertFalse(response.hasActiveInvitation());
    }

    @Test
    void getCustomerResponseById_WithActiveInvitation_Success() {
        AuthToken activeToken = mock(AuthToken.class);
        when(activeToken.isExpired()).thenReturn(false);
        when(mockCustomer.getId()).thenReturn(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(authTokenRepository.findByUserAndType(mockCustomer, TokenType.REGISTRATION))
                .thenReturn(Optional.of(activeToken));

        CustomerResponse response = customerService.getCustomerResponseById(customerId);

        assertNotNull(response);
        assertEquals(customerId, response.id());
        assertTrue(response.hasActiveInvitation());
    }

    @Test
    void getCustomerResponseById_NotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            customerService.getCustomerResponseById(customerId);
        });
    }

    @Test
    void getCustomers_Success() {
        when(customerRepository.findCustomers("Mario", true)).thenReturn(List.of(mockCustomer));
        when(mockCustomer.getId()).thenReturn(customerId);

        List<CustomerListResponse> responses = customerService.getCustomers("Mario", true);

        assertEquals(1, responses.size());
        assertEquals(customerId, responses.get(0).id());
        verify(customerRepository).findCustomers("Mario", true);
    }

    // write

    @Test
    void addCustomer_Success() {
        when(userRepository.findByEmail(customerRequest.email())).thenReturn(Optional.empty());

        CustomerResponse response = customerService.addCustomer(customerRequest);

        assertNotNull(response);
        assertFalse(response.hasActiveInvitation());
        verify(customerRepository).save(any(Customer.class));
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
        when(authTokenRepository.findByUserAndType(mockCustomer, TokenType.REGISTRATION)).thenReturn(Optional.empty());
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
        when(authTokenRepository.findByUserAndType(mockCustomer, TokenType.REGISTRATION)).thenReturn(Optional.empty());
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
        when(authTokenRepository.findByUserAndType(mockCustomer, TokenType.REGISTRATION)).thenReturn(Optional.empty());
        when(mockCustomer.getEmail()).thenReturn("vecchia.email@example.com");

        Customer anotherCustomer = mock(Customer.class);
        when(userRepository.findByEmail(customerRequest.email())).thenReturn(Optional.of(anotherCustomer));

        assertThrows(IllegalStateException.class, () -> {
            customerService.updateCustomer(customerId, customerRequest);
        });

        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateCustomer_FailsIfNullEmailToActiveCustomer() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(authTokenRepository.findByUserAndType(mockCustomer, TokenType.REGISTRATION)).thenReturn(Optional.empty());
        when(mockCustomer.isActive()).thenReturn(true);
        customerRequest = new CustomerRequest(
                "Mario",
                "Rossi",
                "3331234567",
                null,
                "Roma",
                "00100",
                "RSSMRA80A01H501Z",
                null);

        assertThrows(IllegalStateException.class, () -> {
            customerService.updateCustomer(customerId, customerRequest);
        });

        verify(customerRepository, never()).save(any());
    }
}