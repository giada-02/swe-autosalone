package com.autosalone.models;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.autosalone.enums.TransactionType;

// static factory method
public class TransactionFactory {
    private TransactionFactory() {
    }

    public static Transaction createVehiclePurchase(String brand, String model, BigDecimal amount, LocalDate date) {
        String reason = String.format("Acquisto - %s %s", brand, model);
        return new Transaction(reason, amount, date, TransactionType.OUT);
    }

    public static Transaction createVehicleExpense(Vehicle vehicle, String description, BigDecimal amount,
            LocalDate date) {
        String reason = String.format("Spesa - %s %s", vehicle.getBrand(), vehicle.getModel());
        if (description != null)
            reason = reason + ": " + description;
        return new Transaction(reason, amount, date, TransactionType.OUT, vehicle);
    }

    public static Transaction createContractIncome(Contract contract, BigDecimal amount, LocalDate date) {
        String reason = String.format("Vendita Contratto a %s %s di %s %s", contract.getCustomer().getFirstName(),
                contract.getCustomer().getLastName(), contract.getVehicle().getBrand(),
                contract.getVehicle().getModel());
        return new Transaction(reason, amount, date, TransactionType.IN);
    }

    public static Transaction createContractDeposit(Contract contract, BigDecimal amount, LocalDate date) {
        String reason = String.format("Caparra Contratto a %s %s di %s %s", contract.getCustomer().getFirstName(),
                contract.getCustomer().getLastName(), contract.getVehicle().getBrand(),
                contract.getVehicle().getModel());
        return new Transaction(reason, amount, date, TransactionType.IN);
    }

    public static Transaction createContractPayment(Contract contract, String description, BigDecimal amount,
            LocalDate date) {
        String reason = String.format("Pagamento Contratto a %s %s di %s %s", contract.getCustomer().getFirstName(),
                contract.getCustomer().getLastName(), contract.getVehicle().getBrand(),
                contract.getVehicle().getModel());
        if (description != null)
            reason = reason + ": " + description;
        return new Transaction(reason, amount, date, TransactionType.IN, contract);
    }

    public static Transaction createContractRefund(Contract contract, BigDecimal amount, LocalDate date) {
        String reason = String.format("Rimborso Contratto a %s %s di %s %s", contract.getCustomer().getFirstName(),
                contract.getCustomer().getLastName(), contract.getVehicle().getBrand(),
                contract.getVehicle().getModel());
        return new Transaction(reason, amount, date, TransactionType.OUT);
    }

    public static Transaction createGeneralExpense(String reason, BigDecimal amount, LocalDate date) {
        return new Transaction(reason, amount, date, TransactionType.OUT);
    }
}
