package com.Practice.URL.security;

import com.Practice.URL.config.ProductionEnvironmentValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs,
                      Environment environment) {
        this.expirationMs = expirationMs;
        this.key = Keys.hmacShaKeyFor(resolveSecret(secret, environment));
    }

    private static byte[] resolveSecret(String secret, Environment environment) {
        boolean production = ProductionEnvironmentValidator.isProduction(environment);

        if (secret == null || secret.isBlank()) {
            if (production) {
                // The startup validator reports the full requirement; fail fast here as well.
                throw new IllegalStateException(
                        "Missing required environment variable for production: APP_JWT_SECRET");
            }
            byte[] ephemeral = new byte[48];
            new SecureRandom().nextBytes(ephemeral);
            log.warn("app.jwt.secret is not set; generated an ephemeral development signing key. "
                    + "Issued tokens are only valid until the application restarts. "
                    + "Set APP_JWT_SECRET for a stable key.");
            return ephemeral;
        }

        if (ProductionEnvironmentValidator.isForbiddenSecret(secret)) {
            throw new IllegalStateException(
                    "Unsafe JWT secret: app.jwt.secret matches a known development default and must not be used. "
                    + "Generate a new secret (openssl rand -base64 48).");
        }

        if (secret.getBytes(StandardCharsets.UTF_8).length < ProductionEnvironmentValidator.MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "Unsafe JWT secret: app.jwt.secret must be at least "
                            + ProductionEnvironmentValidator.MIN_SECRET_BYTES + " bytes for HS256.");
        }

        return secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Optional<String> extractEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getBody();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
