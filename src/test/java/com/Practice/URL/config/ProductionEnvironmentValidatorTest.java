package com.Practice.URL.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionEnvironmentValidatorTest {

    private final ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator();

    private MockEnvironment productionEnvironment() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.withProperty("DB_USERNAME", "prod_user");
        env.withProperty("DB_PASSWORD", "prod_password_not_real");
        env.withProperty("spring.jpa.hibernate.ddl-auto", "validate");
        env.withProperty("springdoc.api-docs.enabled", "false");
        env.withProperty("springdoc.swagger-ui.enabled", "false");
        env.withProperty("app.dev-user.enabled", "false");
        env.withProperty("APP_JWT_SECRET", randomValidSecret());
        env.withProperty("APP_BASE_URL", "https://links.example.com");
        return env;
    }

    private static String randomValidSecret() {
        byte[] raw = new byte[48];
        new java.security.SecureRandom().nextBytes(raw);
        return java.util.Base64.getEncoder().withoutPadding().encodeToString(raw);
    }

    @Test
    void validProductionConfigurationPasses() {
        assertDoesNotThrow(() -> validator.validate(productionEnvironment()));
    }

    @Test
    void developmentConfigurationWithoutVariablesIsAllowed() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        assertDoesNotThrow(() -> validator.validate(env));
    }

    @Test
    void noProfileConfigurationIsAllowed() {
        assertDoesNotThrow(() -> validator.validate(new MockEnvironment()));
    }

    @Test
    void productionFailsWhenDatabaseUsernameMissing() {
        MockEnvironment env = productionEnvironment();
        env.withProperty("DB_USERNAME", "");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("DB_USERNAME"), ex.getMessage());
    }

    @Test
    void productionFailsWhenDatabasePasswordMissing() {
        MockEnvironment env = productionEnvironment();
        env.withProperty("DB_PASSWORD", "");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("DB_PASSWORD"), ex.getMessage());
    }

    @Test
    void productionFailsWhenJwtSecretMissing() {
        MockEnvironment env = productionEnvironment();
        env.withProperty("APP_JWT_SECRET", "");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("APP_JWT_SECRET"), ex.getMessage());
    }

    @Test
    void productionFailsWhenJwtSecretTooShort() {
        MockEnvironment env = productionEnvironment();
        env.withProperty("APP_JWT_SECRET", "short-secret");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("at least 32 bytes"), ex.getMessage());
    }

    @Test
    void productionFailsWhenJwtSecretIsKnownDevelopmentDefault() {
        MockEnvironment env = productionEnvironment();
        env.withProperty("APP_JWT_SECRET", ProductionEnvironmentValidator.FORBIDDEN_SECRETS.get(0));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("known development default"), ex.getMessage());
    }

    @Test
    void productionFailsWhenBaseUrlMissing() {
        MockEnvironment env = productionEnvironment();
        env.withProperty("APP_BASE_URL", "");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("APP_BASE_URL"), ex.getMessage());
    }

    @Test
    void productionFailsWhenBaseUrlIsNotHttps() {
        MockEnvironment env = productionEnvironment();
        env.withProperty("APP_BASE_URL", "http://insecure.example.com");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("https://"), ex.getMessage());
    }

    @Test
    void productionFailsWhenDdlAutoIsNotValidate() {
        MockEnvironment env = productionEnvironment();
        env.withProperty("spring.jpa.hibernate.ddl-auto", "update");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("validate"), ex.getMessage());
    }

    @Test
    void productionFailsWhenDevUserSeedingEnabled() {
        MockEnvironment env = productionEnvironment();
        env.withProperty("app.dev-user.enabled", "true");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("app.dev-user.enabled"), ex.getMessage());
    }

    @Test
    void productionFailsWhenSwaggerEnabled() {
        MockEnvironment env = productionEnvironment();
        env.withProperty("springdoc.swagger-ui.enabled", "true");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("springdoc"), ex.getMessage());
    }

    @Test
    void productionFailsWhenLocalProfileAlsoActive() {
        MockEnvironment env = productionEnvironment();
        env.setActiveProfiles("prod", "local");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(env));
        assertTrue(ex.getMessage().contains("'local' profile"), ex.getMessage());
    }

    @Test
    void productionProfileDetectionIgnoresCase() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("PROD");
        assertTrue(ProductionEnvironmentValidator.isProduction(env));
    }
}
