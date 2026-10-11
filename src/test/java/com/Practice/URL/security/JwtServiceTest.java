package com.Practice.URL.security;

import com.Practice.URL.config.ProductionEnvironmentValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static String randomValidSecret() {
        byte[] raw = new byte[48];
        new SecureRandom().nextBytes(raw);
        return Base64.getEncoder().withoutPadding().encodeToString(raw);
    }

    private static MockEnvironment developmentEnvironment() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        return env;
    }

    private static MockEnvironment productionEnvironment() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        return env;
    }

    @Test
    void blankSecretInDevelopmentGeneratesEphemeralKeyAndTokensWork() {
        JwtService service = new JwtService("", 60_000, developmentEnvironment());
        String token = service.generateToken("dev@example.com");
        assertEquals("dev@example.com", service.extractEmail(token).orElseThrow());
    }

    @Test
    void blankSecretInProductionFailsStartup() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService("", 60_000, productionEnvironment()));
        assertTrue(ex.getMessage().contains("APP_JWT_SECRET"), ex.getMessage());
    }

    @Test
    void weakSecretIsRejected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService("too-short", 60_000, developmentEnvironment()));
        assertTrue(ex.getMessage().contains("at least 32 bytes"), ex.getMessage());
    }

    @Test
    void knownDevelopmentDefaultSecretIsRejected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService(ProductionEnvironmentValidator.FORBIDDEN_SECRETS.get(0),
                        60_000, developmentEnvironment()));
        assertTrue(ex.getMessage().contains("known development default"), ex.getMessage());
    }

    @Test
    void validSecretIssuesVerifiableTokens() {
        JwtService service = new JwtService(randomValidSecret(), 60_000, developmentEnvironment());
        String token = service.generateToken("user@example.com");
        assertEquals("user@example.com", service.extractEmail(token).orElseThrow());
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtService signer = new JwtService(randomValidSecret(), 60_000, developmentEnvironment());
        JwtService other = new JwtService(randomValidSecret(), 60_000, developmentEnvironment());
        String token = signer.generateToken("user@example.com");
        assertFalse(other.extractEmail(token).isPresent());
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService service = new JwtService(randomValidSecret(), 1, developmentEnvironment());
        String token = service.generateToken("user@example.com");
        Thread.sleep(20);
        assertFalse(service.extractEmail(token).isPresent());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = new JwtService(randomValidSecret(), 60_000, developmentEnvironment());
        String token = service.generateToken("user@example.com");
        String[] parts = token.split("\\.");
        String forged = parts[0] + "." + parts[1] + "." + "AAAA" + parts[2].substring(4);
        assertFalse(service.extractEmail(forged).isPresent());
    }

    @Test
    void expirationWindowIsHonoured() {
        JwtService service = new JwtService(randomValidSecret(), 60_000, developmentEnvironment());
        assertEquals(60_000, service.getExpirationMs());
        String token = service.generateToken("user@example.com");
        // Token must parse now (not expired).
        assertTrue(service.extractEmail(token).isPresent());
        assertTrue(new Date().before(new Date(System.currentTimeMillis() + 60_000)));
        assertNotEquals("", token);
    }
}
