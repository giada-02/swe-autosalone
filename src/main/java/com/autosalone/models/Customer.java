package com.autosalone.models;

import java.util.Locale;

import com.autosalone.utils.Utils;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "customers")
public class Customer extends User {

    // residenza
    @Column(name = "residence_city")
    private String residenceCity;

    @Column(name = "zip_code", length = 5)
    private String zipCode;

    // dati fiscali
    @Size(min = 11, max = 16, message = "Il Codice Fiscale deve essere di 11 o 16 caratteri")
    @Column(name = "fiscal_code", length = 16)
    private String fiscalCode; // codice fiscale

    @Size(min = 11, max = 11, message = "La Partita IVA deve essere di esattamente 11 cifre")
    @Column(name = "vat_number", length = 11)
    private String vatNumber; // partita iva

    protected Customer() {
        super();
    }

    private Customer(CustomerBuilder builder) {
        super(builder);
        this.residenceCity = Utils.sanitizeText(builder.residenceCity);
        this.zipCode = Utils.sanitizeText(builder.zipCode);
        this.fiscalCode = normalizeFiscalCode(builder.fiscalCode);
        this.vatNumber = Utils.sanitizeText(builder.vatNumber);
    }

    // getters
    public String getResidenceCity() {
        return residenceCity;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getFiscalCode() {
        return fiscalCode;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    // setters
    public void setResidenceCity(String residenceCity) {
        this.residenceCity = Utils.sanitizeText(residenceCity);
    }

    public void setZipCode(String zipCode) {
        this.zipCode = Utils.sanitizeText(zipCode);
    }

    public void setFiscalCode(String fiscalCode) {
        this.fiscalCode = normalizeFiscalCode(fiscalCode);
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = Utils.sanitizeText(vatNumber);
    }

    // builder
    public static class CustomerBuilder extends UserBuilder<Customer, CustomerBuilder> {
        private String residenceCity;
        private String zipCode;
        private String fiscalCode;
        private String vatNumber;

        public CustomerBuilder setResidenceCity(String residenceCity) {
            this.residenceCity = residenceCity;
            return self();
        }

        public CustomerBuilder setZipCode(String zipCode) {
            this.zipCode = zipCode;
            return self();
        }

        public CustomerBuilder setFiscalCode(String fiscalCode) {
            this.fiscalCode = fiscalCode;
            return self();
        }

        public CustomerBuilder setVatNumber(String vatNumber) {
            this.vatNumber = vatNumber;
            return self();
        }

        @Override
        public CustomerBuilder self() {
            return this;
        }

        @Override
        public Customer build() {
            return new Customer(this);
        }
    }

    // normalization
    private static String normalizeFiscalCode(String fiscalCode) {
        if (fiscalCode == null || fiscalCode.isBlank()) {
            return null;
        }
        return fiscalCode.trim().toUpperCase(Locale.ROOT);
    }
}
