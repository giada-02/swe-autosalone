package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
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

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

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
    void getCustomerById_Returns200AndCustomer() {
        CustomerResponse mockCustomerResponse = mock(CustomerResponse.class);
        when(mockCustomerResponse.id()).thenReturn(customerId);
        when(mockCustomerResponse.lastName()).thenReturn("Rossi");
        when(mockCustomerResponse.residenceCity()).thenReturn("Roma");
        when(customerService.getCustomerResponseById(customerId)).thenReturn(mockCustomerResponse);

        Response response = customerController.getCustomerById(customerId);

        assertEquals(200, response.getStatus());

        CustomerResponse customerResponse = (CustomerResponse) response.getEntity();
        assertEquals(customerId, customerResponse.id());
        assertEquals("Rossi", customerResponse.lastName());
        assertEquals("Roma", customerResponse.residenceCity());

        verify(customerService).getCustomerResponseById(customerId);
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