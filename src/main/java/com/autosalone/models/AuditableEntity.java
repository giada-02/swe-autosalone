package com.autosalone.models;

import java.time.Instant;
import java.util.UUID;

import com.autosalone.utils.AuditContext;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class AuditableEntity {

    @Version
    @Column(name = "version")
    private Long version; // gestito automaticamente da Hibernate (parte da 0)

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @PrePersist
    public void onPrePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = AuditContext.getCurrentUserId();
        this.updatedBy = AuditContext.getCurrentUserId();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
        this.updatedBy = AuditContext.getCurrentUserId();
    }

    // getters

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    // setters
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }
}
