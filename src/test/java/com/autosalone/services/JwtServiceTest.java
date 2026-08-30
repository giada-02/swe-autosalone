package com.autosalone.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.autosalone.models.Customer;
import com.autosalone.models.Owner;
import com.autosalone.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    private JwtService jwtService;

    private Owner mockOwner;
    private User mockCustomer;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        mockOwner = mock(Owner.class);
        mockCustomer = mock(Customer.class);

        Algorithm testAlgorithm = Algorithm.HMAC256("test-secret-key");
        JWTVerifier testVerifier = JWT.require(testAlgorithm).withIssuer("test-issuer").build();

        injectPrivateField(jwtService, "issuer", "test-issuer");
        injectPrivateField(jwtService, "algorithm", testAlgorithm);
        injectPrivateField(jwtService, "verifier", testVerifier);
    }

    @Test
    void testGenerateAndValidateToken_ForOwner() {
        UUID ownerId = UUID.randomUUID();
        when(mockOwner.getId()).thenReturn(ownerId);
        when(mockOwner.getEmail()).thenReturn("owner@autosalone.com");

        String token = jwtService.generateToken(mockOwner);
        DecodedJWT decodedJWT = jwtService.validateToken(token);

        assertNotNull(token);
        assertEquals(ownerId.toString(), decodedJWT.getSubject());
        assertEquals("OWNER", decodedJWT.getClaim("role").asString());
        assertEquals("owner@autosalone.com", decodedJWT.getClaim("email").asString());
        assertEquals("test-issuer", decodedJWT.getIssuer());
    }

    @Test
    void testGenerateAndValidateToken_ForCustomer() {
        UUID customerId = UUID.randomUUID();
        when(mockCustomer.getId()).thenReturn(customerId);
        when(mockCustomer.getEmail()).thenReturn("customer@email.com");

        String token = jwtService.generateToken(mockCustomer);
        DecodedJWT decodedJWT = jwtService.validateToken(token);

        assertEquals("CUSTOMER", decodedJWT.getClaim("role").asString());
    }

    @Test
    void testValidateToken_InvalidToken_ThrowsException() {
        String fakeToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.fakePayload.fakeSignature";

        assertThrows(JWTVerificationException.class, () -> {
            jwtService.validateToken(fakeToken);
        }, "An invalid token must throw a JWTVerificationException");
    }

    private void injectPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}