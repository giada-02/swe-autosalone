package com.autosalone.models;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer extends User {

    @Column(name = "residence_city")
    private String residenceCity;

    @Column(name = "is_active")
    private boolean isActive;

    protected Customer() {
        super();
    }

    private Customer(CustomerBuilder builder) {
        super(builder);
        this.residenceCity = builder.residenceCity;
        this.isActive = false;
    }

    public String getResidenceCity() {
        return residenceCity;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setResidenceCity(String residenceCity) {
        this.residenceCity = residenceCity;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public static class CustomerBuilder extends UserBuilder<Customer, CustomerBuilder> {
        private String residenceCity;

        public CustomerBuilder setResidenceCity(String residenceCity) {
            this.residenceCity = residenceCity;
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
