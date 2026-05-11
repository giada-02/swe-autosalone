package com.autosalone.models;

public class Owner extends User {

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
