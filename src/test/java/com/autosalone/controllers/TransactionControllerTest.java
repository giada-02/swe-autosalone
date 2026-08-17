package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.requests.GeneralTransactionRequest;
import com.autosalone.dtos.responses.TransactionResponse;
import com.autosalone.enums.SortOrder;
import com.autosalone.enums.TransactionType;
import com.autosalone.services.TransactionService;

import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private LocalDate now;
    private UUID transactionId;
    private TransactionResponse transactionResponse;
    private GeneralTransactionRequest expenseRequest;
    private GeneralTransactionRequest incomeRequest;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();
        transactionId = UUID.randomUUID();

        transactionResponse = new TransactionResponse(transactionId, "Descrizione", new BigDecimal("150.00"),
                now.toString(), TransactionType.OUT, null, null);

        expenseRequest = new GeneralTransactionRequest("Spesa ufficio", new BigDecimal("150.00"), now);
        incomeRequest = new GeneralTransactionRequest("Consulenza", new BigDecimal("500.00"), now);
    }

    @Test
    void getTransactions_Returns200AndList() {
        LocalDate dateFrom = now.minusDays(10);
        LocalDate dateTo = now;
        when(transactionService.getTransactions(dateFrom, dateTo, TransactionType.OUT, SortOrder.DESC))
                .thenReturn(List.of(transactionResponse));

        Response response = transactionController.getTransactions(dateFrom.toString(), dateTo.toString(),
                TransactionType.OUT, SortOrder.DESC);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(transactionService).getTransactions(dateFrom, dateTo, TransactionType.OUT, SortOrder.DESC);
    }

    @Test
    void getTransactionById_Returns200AndTransaction() {
        when(transactionService.getTransactionResponseById(transactionId)).thenReturn(transactionResponse);

        Response response = transactionController.getTransactionById(transactionId);

        assertEquals(200, response.getStatus());
        assertEquals(transactionResponse, response.getEntity());
        verify(transactionService).getTransactionResponseById(transactionId);
    }

    @Test
    void getSummary_Returns200AndMapWithTotals() {
        LocalDate dateFrom = now.minusDays(10);
        LocalDate dateTo = now;

        when(transactionService.getSumOfIncomes(dateFrom, dateTo)).thenReturn(new BigDecimal("1000.00"));
        when(transactionService.getSumOfExpenses(dateFrom, dateTo)).thenReturn(new BigDecimal("200.00"));

        Response response = transactionController.getSummary(dateFrom.toString(), dateTo.toString());

        assertEquals(200, response.getStatus());
        Map<?, ?> entity = (Map<?, ?>) response.getEntity();
        assertEquals(new BigDecimal("1000.00"), entity.get("totalIncomes"));
        assertEquals(new BigDecimal("200.00"), entity.get("totalExpenses"));
    }

    @Test
    void createGeneralExpense_Returns201AndLocationHeader() {
        when(transactionService.createGeneralExpense(expenseRequest.reason(), expenseRequest.amount(),
                expenseRequest.date()))
                .thenReturn(transactionResponse);

        Response response = transactionController.createGeneralExpense(expenseRequest);

        assertEquals(201, response.getStatus());
        assertEquals(transactionResponse, response.getEntity());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/transactions/" + transactionId));
    }

    @Test
    void createGeneralIncome_Returns201AndLocationHeader() {
        when(transactionService.createGeneralIncome(incomeRequest.reason(), incomeRequest.amount(),
                incomeRequest.date()))
                .thenReturn(transactionResponse);

        Response response = transactionController.createGeneralIncome(incomeRequest);

        assertEquals(201, response.getStatus());
        assertEquals(transactionResponse, response.getEntity());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/transactions/" + transactionId));
    }
}