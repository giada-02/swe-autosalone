package com.autosalone.models;

import jakarta.persistence.*;

@Entity
@Table(name = "owners")
public class Owner extends User {

    protected Owner() {
        super();
    }

    private Owner(OwnerBuilder builder) {
        super(builder);
    }

    public static class OwnerBuilder extends UserBuilder<Owner, OwnerBuilder> {

        @Override
        public OwnerBuilder self() {
            return this;
        }

        @Override
        public Owner build() {
            return new Owner(this);
        }

    }

}
