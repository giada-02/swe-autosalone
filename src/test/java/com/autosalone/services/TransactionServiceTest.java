package com.autosalone.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.responses.ExpenseResponse;
import com.autosalone.dtos.responses.TransactionResponse;
import com.autosalone.enums.SortOrder;
import com.autosalone.enums.TransactionType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Transaction;
import com.autosalone.models.Vehicle;
import com.autosalone.repositories.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private UUID transactionId;
    private UUID vehicleId;
    private UUID contractId;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        contractId = UUID.randomUUID();
        mockTransaction = mock(Transaction.class);
    }

    // read

    @Test
    void getTransactionById_Success() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTransaction));
        Transaction result = transactionService.getTransactionById(transactionId);
        assertNotNull(result);
        assertEquals(mockTransaction, result);
    }

    @Test
    void getTransactionById_NotFound() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.getTransactionById(transactionId);
        });
    }

    @Test
    void getTransactions_Success() {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        when(transactionRepository.findTransactions(from, to, TransactionType.IN, SortOrder.DESC))
                .thenReturn(List.of(mockTransaction));

        List<TransactionResponse> results = transactionService.getTransactions(from, to, TransactionType.IN,
                SortOrder.DESC);

        assertEquals(1, results.size());
        verify(transactionRepository).findTransactions(from, to, TransactionType.IN, SortOrder.DESC);
    }

    @Test
    void getExpensesByVehicleId_Success() {
        when(transactionRepository.findAllExpenses(vehicleId)).thenReturn(List.of(mockTransaction));
        Vehicle mockVehicle = mock(Vehicle.class);
        when(mockTransaction.getVehicle()).thenReturn(mockVehicle);

        List<ExpenseResponse> results = transactionService.getExpensesByVehicleId(vehicleId);

        assertEquals(1, results.size());
        verify(transactionRepository).findAllExpenses(vehicleId);
    }

    @Test
    void getPaymentsByContractId_Success() {
        when(transactionRepository.findAllPayments(contractId)).thenReturn(List.of(mockTransaction));

        List<TransactionResponse> results = transactionService.getPaymentsByContractId(contractId);

        assertEquals(1, results.size());
        verify(transactionRepository).findAllPayments(contractId);
    }

    @Test
    void getSumOfIncomes_Success() {
        LocalDate from = LocalDate.now().minusDays(10);
        LocalDate to = LocalDate.now();
        BigDecimal expectedSum = BigDecimal.valueOf(5000);

        when(transactionRepository.sumInTransactions(from, to)).thenReturn(expectedSum);

        BigDecimal result = transactionService.getSumOfIncomes(from, to);

        assertEquals(expectedSum, result);
        verify(transactionRepository).sumInTransactions(from, to);
    }

    @Test
    void getSumOfExpenses_Success() {
        LocalDate from = LocalDate.now().minusDays(10);
        LocalDate to = LocalDate.now();
        BigDecimal expectedSum = BigDecimal.valueOf(1500);

        when(transactionRepository.sumOutTransactions(from, to)).thenReturn(expectedSum);

        BigDecimal result = transactionService.getSumOfExpenses(from, to);

        assertEquals(expectedSum, result);
        verify(transactionRepository).sumOutTransactions(from, to);
    }

    // write

    @Test
    void createGeneralExpense_Success() {
        String reason = "Bolletta Luce";
        BigDecimal amount = BigDecimal.valueOf(250.50);
        LocalDate date = LocalDate.now();

        TransactionResponse response = transactionService.createGeneralExpense(reason, amount, date);

        assertEquals(reason, response.reason());
        assertEquals(amount, response.amount());
        assertEquals(date.toString(), response.date());
        assertEquals(TransactionType.OUT, response.type());

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createGeneralIncome_Success() {
        String reason = "Rimborso Fiscale";
        BigDecimal amount = BigDecimal.valueOf(1000.00);
        LocalDate date = LocalDate.now();

        TransactionResponse response = transactionService.createGeneralIncome(reason, amount, date);

        assertEquals(reason, response.reason());
        assertEquals(amount, response.amount());
        assertEquals(date.toString(), response.date());
        assertEquals(TransactionType.IN, response.type());

        verify(transactionRepository).save(any(Transaction.class));
    }
}