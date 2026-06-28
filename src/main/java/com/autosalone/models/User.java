package com.autosalone.models;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    protected User() {
    }

    protected User(UserBuilder<?, ?> builder) {
        Objects.requireNonNull(builder.firstName);
        Objects.requireNonNull(builder.lastName);
        Objects.requireNonNull(builder.phoneNumber);

        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.phoneNumber = builder.phoneNumber;
        this.email = builder.email;
        this.password = builder.password;
        this.isActive = builder.isActive;
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

    public boolean isActive() {
        return isActive;
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

    public void setPassword(String newPassword) {
        this.password = newPassword;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public abstract static class UserBuilder<T extends User, B extends UserBuilder<T, B>> {
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String email;
        private String password;
        private boolean isActive = false;

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

        public B setIsActive(boolean isActive) {
            this.isActive = isActive;
            return self();
        }

        protected abstract B self();

        protected abstract T build();
    }
}