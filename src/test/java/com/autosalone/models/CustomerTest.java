package com.autosalone.models;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.autosalone.models.Customer.CustomerBuilder;

public class CustomerTest {

    private Customer customer;

    @BeforeEach
    public void setUp() {
        customer = new CustomerBuilder().setFirstName("Luigi").setLastName("Verdi")
                .setPhoneNumber("1234567890")
                .build();
    }

    @Test
    public void builder_NormalizesData_Success() {
        Customer messyCustomer = new CustomerBuilder().setFirstName("Luigi").setLastName("Verdi")
                .setPhoneNumber("1234567890").setResidenceCity(" Roma  ").setZipCode("  00100 ")
                .setFiscalCode(" VRdlgU85E15h501R").setVatNumber(" 2472198357 ")
                .build();

        assertAll(
                () -> assertEquals("Roma", messyCustomer.getResidenceCity()),
                () -> assertEquals("00100", messyCustomer.getZipCode()),
                () -> assertEquals("VRDLGU85E15H501R", messyCustomer.getFiscalCode()),
                () -> assertEquals("2472198357", messyCustomer.getVatNumber()));
    }

    @Test
    public void setResidenceCity_NormalizesValue() {
        customer.setResidenceCity(" Roma  ");

        assertEquals("Roma", customer.getResidenceCity());
    }

    @Test
    public void setResidenceCity_Null_SetsNull() {
        customer.setResidenceCity("Roma");

        customer.setResidenceCity(null);

        assertNull(customer.getResidenceCity());
    }

    @Test
    public void setResidenceCity_Blank_SetsNull() {
        customer.setResidenceCity("Roma");

        customer.setResidenceCity("  ");

        assertNull(customer.getResidenceCity());
    }

    @Test
    public void setZipCode_NormalizesValue() {
        customer.setZipCode("  00100 ");

        assertEquals("00100", customer.getZipCode());
    }

    @Test
    public void setZipCode_Null_SetsNull() {
        customer.setZipCode("00100");

        customer.setZipCode(null);

        assertNull(customer.getZipCode());
    }

    @Test
    public void setZipCode_Blank_SetsNull() {
        customer.setZipCode("00100");

        customer.setZipCode("  ");

        assertNull(customer.getZipCode());
    }

    @Test
    public void setFiscalCode_NormalizesValue() {
        customer.setFiscalCode(" VRdlgU85E15h501R");

        assertEquals("VRDLGU85E15H501R", customer.getFiscalCode());
    }

    @Test
    public void setFiscalCode_Null_SetsNull() {
        customer.setFiscalCode("VRDLGU85E15H501R");

        customer.setFiscalCode(null);

        assertNull(customer.getFiscalCode());
    }

    @Test
    public void setFiscalCode_Blank_SetsNull() {
        customer.setFiscalCode("VRDLGU85E15H501R");

        customer.setFiscalCode("  ");

        assertNull(customer.getFiscalCode());
    }

    @Test
    public void setVatNumber_NormalizesValue() {
        customer.setVatNumber(" 2472198357 ");

        assertEquals("2472198357", customer.getVatNumber());
    }

    @Test
    public void setVatNumber_Null_SetsNull() {
        customer.setVatNumber("2472198357");

        customer.setVatNumber(null);

        assertNull(customer.getVatNumber());
    }

    @Test
    public void setVatNumber_Blank_SetsNull() {
        customer.setVatNumber("2472198357");

        customer.setVatNumber("  ");

        assertNull(customer.getVatNumber());
    }
}