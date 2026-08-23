package com.autosalone.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.identitystore.PasswordHash;
import java.util.Map;

@ApplicationScoped
public class BCryptPasswordHash implements PasswordHash {

    private static final int COST_FACTOR = 12;

    @Override
    public void initialize(Map<String, String> parameters) {
        // No external configuration needed
    }

    @Override
    public String generate(char[] password) {
        // BCrypt automatically generates a secure 16-byte salt
        // and bakes the cost factor directly into the output string
        return BCrypt.withDefaults().hashToString(COST_FACTOR, password);
    }

    @Override
    public boolean verify(char[] password, String hashedPassword) {
        // The verifier automatically reads the cost factor and salt
        // straight from the stored hash string in the database
        return BCrypt.verifyer().verify(password, hashedPassword).verified;
    }
}