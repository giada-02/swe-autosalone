package com.autosalone.models;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue(value = "CUSTOMER")
public class Customer extends User {

    @Column(name = "hometown")
    private String hometown; // città di residenza

    @Column(name = "is_active")
    private boolean isActive;

    protected Customer() {
        super();
    }

    private Customer(CustomerBuilder builder) {
        super(builder);
        this.hometown = builder.hometown;
        this.isActive = false;
    }

    public String getHometown() {
        return hometown;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public static class CustomerBuilder extends UserBuilder<Customer, CustomerBuilder> {
        private String hometown;

        public CustomerBuilder setHometown(String hometown) {
            this.hometown = hometown;
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
