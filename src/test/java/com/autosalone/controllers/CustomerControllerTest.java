package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;
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
import com.autosalone.services.CustomerService;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Principal principal;

    @InjectMocks
    private CustomerController customerController;

    private UUID customerId;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customerRequest = new CustomerRequest("Mario", "Rossi", "12345", "test@test.com", "Roma", "00100", "CF", null);
    }

    @Test
    void getCustomers_Returns200AndList() {
        CustomerListResponse customerListResponse = new CustomerListResponse(
                customerId, "Mario", "Rossi", "12345", "test@test.com",
                true, "Roma", "00100", "CF", null);
        when(customerService.getCustomers("Mario", true)).thenReturn(List.of(customerListResponse));

        Response response = customerController.getCustomers("Mario", true);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(customerService).getCustomers("Mario", true);
    }

    @Test
    void getCustomerById_AsOwner_Returns200() {
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(UUID.randomUUID().toString());
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(false);

        CustomerResponse mockCustomerResponse = mock(CustomerResponse.class);
        when(customerService.getCustomerResponseById(customerId)).thenReturn(mockCustomerResponse);

        Response response = customerController.getCustomerById(customerId);

        assertEquals(200, response.getStatus());
    }

    @Test
    void getCustomerById_AsCustomer_Returns403WhenAccessingAnotherProfile() {
        UUID anotherCustomerId = UUID.randomUUID();

        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(true);

        Response response = customerController.getCustomerById(anotherCustomerId);

        assertEquals(403, response.getStatus());
        verify(customerService, never()).getCustomerResponseById(any());
    }

    @Test
    void addCustomer_Returns201AndLocationHeaderWithBody() {
        CustomerResponse mockCustomerResponse = mock(CustomerResponse.class);
        when(mockCustomerResponse.id()).thenReturn(customerId);
        when(customerService.addCustomer(customerRequest)).thenReturn(mockCustomerResponse);

        Response response = customerController.addCustomer(customerRequest);

        assertEquals(201, response.getStatus());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/customers/" + customerId));

        assertEquals(mockCustomerResponse, response.getEntity());
    }

    @Test
    void updateCustomer_Returns200AndUpdatedCustomer() {
        CustomerResponse mockCustomerResponse = mock(CustomerResponse.class);
        when(customerService.updateCustomer(customerId, customerRequest)).thenReturn(mockCustomerResponse);

        Response response = customerController.updateCustomer(customerId, customerRequest);

        assertEquals(200, response.getStatus());
        assertEquals(mockCustomerResponse, response.getEntity());
        verify(customerService).updateCustomer(customerId, customerRequest);
    }
}