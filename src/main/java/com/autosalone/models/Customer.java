package com.autosalone.models;

public class Customer extends User {
    private String hometown; // città di residenza
    private boolean isActive;

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
