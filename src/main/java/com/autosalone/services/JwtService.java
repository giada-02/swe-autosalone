package com.autosalone.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.autosalone.models.Owner;
import com.autosalone.models.User;
import io.github.cdimascio.dotenv.Dotenv;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class JwtService {

    private static final int EXPIRATION_HOURS = 24;

    private String issuer;
    private Algorithm algorithm;
    private JWTVerifier verifier;

    public JwtService() {
    }

    @PostConstruct
    protected void init() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String secretKey = dotenv.get("JWT_SECRET_KEY");
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("CRITICAL: JWT_SECRET_KEY is missing!");
        }

        String envIssuer = dotenv.get("JWT_ISSUER");
        this.issuer = (envIssuer == null || envIssuer.isBlank()) ? "autosalone-api" : envIssuer;

        this.algorithm = Algorithm.HMAC256(secretKey);

        this.verifier = JWT.require(this.algorithm)
                .withIssuer(this.issuer)
                .build();
    }

    public String generateToken(User user) {
        String role = user instanceof Owner ? "OWNER" : "CUSTOMER";
        return JWT.create()
                .withIssuer(this.issuer)
                .withSubject(user.getId().toString())
                .withClaim("role", role)
                .withClaim("email", user.getEmail())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(EXPIRATION_HOURS, ChronoUnit.HOURS))
                .sign(this.algorithm);
    }

    public DecodedJWT validateToken(String token) {
        return this.verifier.verify(token);
    }
}