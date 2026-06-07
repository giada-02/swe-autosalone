package com.autosalone.models;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "customer_snapshots")
public class CustomerSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // dati identificativi e fiscali
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "fiscal_code", nullable = false, length = 16)
    private String fiscalCode;

    @Column(name = "vat_number", length = 11)
    private String vatNumber;

    // residenza
    @Column(name = "zip_code", length = 5)
    private String zipCode;

    @Column(name = "residence_city")
    private String residenceCity;

    // contatti
    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    protected CustomerSnapshot() {
    }

    /// Costruttore di copia
    public CustomerSnapshot(Customer customer) {
        java.util.Objects.requireNonNull(customer, "Cannot create snapshot from null customer");

        this.firstName = customer.getFirstName();
        this.lastName = customer.getLastName();
        this.fiscalCode = customer.getFiscalCode();
        this.vatNumber = customer.getVatNumber();
        this.zipCode = customer.getZipCode();
        this.residenceCity = customer.getResidenceCity();
        this.email = customer.getEmail();
        this.phoneNumber = customer.getPhoneNumber();
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

    public String getFiscalCode() {
        return fiscalCode;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getResidenceCity() {
        return residenceCity;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}