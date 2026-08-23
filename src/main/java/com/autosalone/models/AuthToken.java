package com.autosalone.models;

import java.time.Instant;
import java.util.UUID;

import com.autosalone.enums.TokenType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_tokens")
public class AuthToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenType type;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    protected AuthToken() {
    }

    public AuthToken(String token, User user, TokenType type, Instant expiryDate) {
        this.token = token;
        this.user = user;
        this.type = type;
        this.expiryDate = expiryDate;
    }

    // getters
    @Override
    public UUID getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public TokenType getType() {
        return type;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }
}