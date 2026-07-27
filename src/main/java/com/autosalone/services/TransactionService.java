package com.autosalone.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autosalone.enums.SortOrder;
import com.autosalone.enums.TransactionType;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Transaction;
import com.autosalone.models.TransactionFactory;
import com.autosalone.repositories.TransactionRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TransactionService {

    @Inject
    private TransactionRepository transactionRepository;

    // read

    public Transaction getTransactionById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found of id: " + id));
    }

    public List<Transaction> getTransactions(LocalDate dateFrom,
            LocalDate dateTo,
            TransactionType type,
            SortOrder sortOrder) {
        return transactionRepository.findTransactions(dateFrom, dateTo, type, sortOrder);
    }

    public List<Transaction> getExpensesByVehicleId(UUID vehicleId) {
        return transactionRepository.findAllExpenses(vehicleId);
    }

    public List<Transaction> getPaymentsByContractId(UUID contractId) {
        return transactionRepository.findAllPayments(contractId);
    }

    public BigDecimal getSumOfIncomes(LocalDate dateFrom, LocalDate dateTo) {
        return transactionRepository.sumInTransactions(dateFrom, dateTo);
    }

    public BigDecimal getSumOfExpenses(LocalDate dateFrom, LocalDate dateTo) {
        return transactionRepository.sumOutTransactions(dateFrom, dateTo);
    }

    // write

    @Transactional
    public UUID createGeneralExpense(String reason, BigDecimal amount, LocalDate date) {
        Transaction generalExpense = TransactionFactory.createGeneralExpense(reason, amount, date);

        transactionRepository.save(generalExpense);

        return generalExpense.getId();
    }

    @Transactional
    public UUID createGeneralIncome(String reason, BigDecimal amount, LocalDate date) {
        Transaction generalIncome = TransactionFactory.createGeneralIncome(reason, amount, date);

        transactionRepository.save(generalIncome);

        return generalIncome.getId();
    }
}
