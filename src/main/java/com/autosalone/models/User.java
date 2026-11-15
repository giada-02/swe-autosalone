package com.autosalone.models;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
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

    @Column(name = "tokens_valid_after")
    private Instant tokensValidAfter;

    protected User() {
    }

    protected User(UserBuilder<?, ?> builder) {
        this.firstName = requireName(builder.firstName, "First name is required");
        this.lastName = requireName(builder.lastName, "Last name is required");
        this.phoneNumber = requirePhoneNumber(builder.phoneNumber);
        this.email = normalizeEmail(builder.email);
        this.password = builder.password;
        this.isActive = builder.isActive;
        this.tokensValidAfter = null;
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

    public boolean acceptsTokenIssuedAt(Instant issuedAt) {
        return this.tokensValidAfter == null || !issuedAt.isBefore(this.tokensValidAfter);
    }

    // setters
    public void setFirstName(String firstName) {
        this.firstName = requireName(firstName, "First name is required");
    }

    public void setLastName(String lastName) {
        this.lastName = requireName(lastName, "Last name is required");
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = requirePhoneNumber(phoneNumber);
    }

    public void setEmail(String email) {
        this.email = normalizeEmail(email);
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

    public void invalidateTokens(Instant now) {
        this.tokensValidAfter = now.truncatedTo(ChronoUnit.SECONDS);
    }

    protected abstract static class UserBuilder<T extends User, B extends UserBuilder<T, B>> {
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

    public boolean hasSameEmail(String email) {
        return normalizeEmail(email) != null && Objects.equals(this.email, normalizeEmail(email));
    }

    // validation and normalization
    private static String requireName(String name, String message) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException(message);
        return normalizeName(name);
    }

    private static String requirePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank())
            throw new IllegalArgumentException("Phone number is required");
        return normalizePhoneNumber(phoneNumber);
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank())
            return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private static String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("[\\s\\-]", "");
    }
}