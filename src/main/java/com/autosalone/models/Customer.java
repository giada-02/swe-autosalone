package com.autosalone.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "customers")
public class Customer extends User {

    @Column(name = "residence_city")
    private String residenceCity;

    @Column(name = "is_active")
    private boolean isActive;

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
        this.residenceCity = builder.residenceCity;
        this.fiscalCode = builder.fiscalCode;
        this.vatNumber = builder.vatNumber;
        this.isActive = false;
    }

    // getters
    public String getResidenceCity() {
        return residenceCity;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getFiscalCode() {
        return fiscalCode;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    // setters
    public void setResidenceCity(String residenceCity) {
        this.residenceCity = residenceCity;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void setFiscalCode(String fiscalCode) {
        this.fiscalCode = fiscalCode;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    // builder
    public static class CustomerBuilder extends UserBuilder<Customer, CustomerBuilder> {
        private String residenceCity;
        private String fiscalCode;
        private String vatNumber;

        public CustomerBuilder setResidenceCity(String residenceCity) {
            this.residenceCity = residenceCity;
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

}
