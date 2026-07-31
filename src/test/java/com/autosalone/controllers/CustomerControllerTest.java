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

import com.autosalone.dtos.CustomerRequest;
import com.autosalone.dtos.CustomerResponse;
import com.autosalone.models.Customer;
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
        Customer mockCustomer = mock(Customer.class);
        when(customerService.getCustomers("Mario", true)).thenReturn(List.of(mockCustomer));

        Response response = customerController.getCustomers("Mario", true);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(customerService).getCustomers("Mario", true);
    }

    @Test
    void getCustomerById_Returns200AndCustomer() {
        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn(customerId);
        when(mockCustomer.getLastName()).thenReturn("Rossi");
        when(mockCustomer.getResidenceCity()).thenReturn("Roma");
        when(customerService.getCustomerById(customerId)).thenReturn(mockCustomer);

        Response response = customerController.getCustomerById(customerId);

        assertEquals(200, response.getStatus());

        CustomerResponse customerResponse = (CustomerResponse) response.getEntity();
        assertEquals(customerId, customerResponse.id());
        assertEquals("Rossi", customerResponse.lastName());
        assertEquals("Roma", customerResponse.residenceCity());

        verify(customerService).getCustomerById(customerId);
    }

    @Test
    void addCustomer_Returns201AndLocationHeader() {
        when(customerService.addCustomer(customerRequest)).thenReturn(customerId);

        Response response = customerController.addCustomer(customerRequest);

        assertEquals(201, response.getStatus());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/customers/" + customerId));
    }

    @Test
    void updateCustomer_Returns204NoContent() {
        Response response = customerController.updateCustomer(customerId, customerRequest);

        assertEquals(204, response.getStatus());
        assertNull(response.getEntity()); // no body
        verify(customerService).updateCustomer(customerId, customerRequest);
    }
}