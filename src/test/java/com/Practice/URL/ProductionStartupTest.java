package com.Practice.URL;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Startup-level checks: production refuses to start without required
 * configuration, and port/database settings resolve from the environment.
 */
class ProductionStartupTest {

    private static String randomValidSecret() {
        byte[] raw = new byte[48];
        new SecureRandom().nextBytes(raw);
        return Base64.getEncoder().withoutPadding().encodeToString(raw);
    }

    private static StandardEnvironment environmentWith(Map<String, Object> overrides) {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("testOverrides", overrides));
        return env;
    }

    private static String allMessages(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            sb.append(current.getMessage()).append('\n');
            current = current.getCause();
        }
        return sb.toString();
    }

    @Test
    void productionStartupFailsWhenRequiredVariablesAreMissing() {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("DB_USERNAME", "");
        overrides.put("DB_PASSWORD", "");
        overrides.put("APP_JWT_SECRET", "");
        overrides.put("APP_BASE_URL", "");

        Exception ex = assertThrows(Exception.class, () ->
                new SpringApplicationBuilder(UrlApplication.class)
                        .web(WebApplicationType.NONE)
                        .environment(environmentWith(overrides))
                        .profiles("prod")
                        .run());

        String messages = allMessages(ex);
        assertTrue(messages.contains("DB_USERNAME"), messages);
    }

    @Test
    void productionStartupRejectsWeakJwtSecret() {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("DB_USERNAME", "user");
        overrides.put("DB_PASSWORD", "password");
        overrides.put("APP_JWT_SECRET", "weak");
        overrides.put("APP_BASE_URL", "https://links.example.com");

        Exception ex = assertThrows(Exception.class, () ->
                new SpringApplicationBuilder(UrlApplication.class)
                        .web(WebApplicationType.NONE)
                        .environment(environmentWith(overrides))
                        .profiles("prod")
                        .run());

        String messages = allMessages(ex);
        assertTrue(messages.contains("APP_JWT_SECRET") || messages.contains("JWT secret"), messages);
    }

    @Test
    void productionStartupRejectsKnownDevelopmentDefaultSecret() {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("DB_USERNAME", "user");
        overrides.put("DB_PASSWORD", "password");
        overrides.put("APP_JWT_SECRET",
                com.Practice.URL.config.ProductionEnvironmentValidator.FORBIDDEN_SECRETS.get(0));
        overrides.put("APP_BASE_URL", "https://links.example.com");

        Exception ex = assertThrows(Exception.class, () ->
                new SpringApplicationBuilder(UrlApplication.class)
                        .web(WebApplicationType.NONE)
                        .environment(environmentWith(overrides))
                        .profiles("prod")
                        .run());

        String messages = allMessages(ex);
        assertTrue(messages.contains("development default") || messages.contains("JWT secret"), messages);
    }

    @Test
    void productionStartupRejectsNonHttpsBaseUrl() {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("DB_USERNAME", "user");
        overrides.put("DB_PASSWORD", "password");
        overrides.put("APP_JWT_SECRET", randomValidSecret());
        overrides.put("APP_BASE_URL", "http://insecure.example.com");

        Exception ex = assertThrows(Exception.class, () ->
                new SpringApplicationBuilder(UrlApplication.class)
                        .web(WebApplicationType.NONE)
                        .environment(environmentWith(overrides))
                        .profiles("prod")
                        .run());

        String messages = allMessages(ex);
        assertTrue(messages.contains("APP_BASE_URL"), messages);
    }

    @Test
    void portAndDatabaseSettingsResolveFromEnvironment() {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("PORT", "6061");
        overrides.put("DB_URL", "jdbc:postgresql://localhost:5432/urlshortener");

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(UrlApplication.class)
                .web(WebApplicationType.NONE)
                .environment(environmentWith(overrides))
                .profiles("local")
                .run()) {

            assertEquals("6061", context.getEnvironment().getProperty("server.port"));
            assertEquals("jdbc:postgresql://localhost:5432/urlshortener",
                    context.getEnvironment().getProperty("spring.datasource.url"));
            // Local profile supplies the untracked development credentials.
            assertTrue(context.getEnvironment().getProperty("spring.datasource.username") != null
                    && !context.getEnvironment().getProperty("spring.datasource.username").isBlank());
        }
    }
}
