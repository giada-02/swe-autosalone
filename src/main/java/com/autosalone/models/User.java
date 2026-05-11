package com.autosalone.models;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role")
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    
    @Column(name = "email")
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    protected User(){}

    protected User(UserBuilder<?, ?> builder) {
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.phoneNumber = builder.phoneNumber;
        this.email = builder.email;
        this.password = builder.password;
    }

    // getters
    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    // setters
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public abstract static class UserBuilder<T extends User, B extends UserBuilder<T, B>> {
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String email;
        private String password;

        public B setFirstName(String firstName) {
            this.firstName = firstName;
            return self();
        }

        public B setLastName(String lastName) {
            this.lastName = lastName;
            return self();
        }

        public B setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return self();
        }

        public B setEmail(String email) {
            this.email = email;
            return self();
        }

        public B setPassword(String password) {
            this.password = password;
            return self();
        }

        protected abstract B self();

        protected abstract T build();
    }
}