package com.Practice.URL.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Fails application startup when the production profile is active but required
 * configuration is missing, weak, or still using a known development value.
 *
 * Runs before any bean is created (EnvironmentPostProcessor), so a misconfigured
 * production deployment stops during startup instead of running unsafely.
 */
public class ProductionEnvironmentValidator implements EnvironmentPostProcessor, Ordered {

    /** Secrets that once shipped as development defaults and must never be used. */
    public static final List<String> FORBIDDEN_SECRETS = List.of(
            "change-me-in-production-please-use-a-long-random-secret-at-least-32-bytes");

    public static final int MIN_SECRET_BYTES = 32;

    private static final Set<String> PRODUCTION_PROFILES = new HashSet<>(Arrays.asList("prod", "production"));

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, org.springframework.boot.SpringApplication application) {
        validate(environment);
    }

    /**
     * Visible for tests. Throws {@link IllegalStateException} with a safe message
     * (variable names only, never secret values) when production config is unsafe.
     */
    void validate(ConfigurableEnvironment environment) {
        if (!isProduction(environment)) {
            return;
        }

        if (isLocal(environment)) {
            throw new IllegalStateException(
                    "Unsafe production configuration: the 'local' profile must not be active in production. "
                            + "Unset SPRING_PROFILES_ACTIVE/local profile before deploying.");
        }

        requirePresent(environment, "DB_USERNAME");
        requirePresent(environment, "DB_PASSWORD");

        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto", "");
        if (!"validate".equals(ddlAuto)) {
            throw new IllegalStateException(
                    "Unsafe production configuration: spring.jpa.hibernate.ddl-auto must be 'validate' in production "
                            + "(current value: '" + ddlAuto + "'). Activate the 'prod' profile.");
        }

        String docsEnabled = environment.getProperty("springdoc.api-docs.enabled", "");
        if ("true".equalsIgnoreCase(docsEnabled)) {
            throw new IllegalStateException(
                    "Unsafe production configuration: springdoc.api-docs.enabled must be false in production.");
        }
        String swaggerEnabled = environment.getProperty("springdoc.swagger-ui.enabled", "");
        if ("true".equalsIgnoreCase(swaggerEnabled)) {
            throw new IllegalStateException(
                    "Unsafe production configuration: springdoc.swagger-ui.enabled must be false in production.");
        }

        String devUserEnabled = environment.getProperty("app.dev-user.enabled", "");
        if ("true".equalsIgnoreCase(devUserEnabled)) {
            throw new IllegalStateException(
                    "Unsafe production configuration: app.dev-user.enabled must be false in production.");
        }

        validateJwtSecretValue(environment.getProperty("APP_JWT_SECRET"));

        String baseUrl = environment.getProperty("APP_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable for production: APP_BASE_URL "
                            + "(public https URL of the deployment, e.g. https://links.example.com)");
        }
        if (!baseUrl.startsWith("https://")) {
            throw new IllegalStateException(
                    "Unsafe production configuration: APP_BASE_URL must start with https:// "
                            + "(current value is not an https URL).");
        }
    }

    /** Shared with JwtService so the same development secrets are rejected everywhere. */
    public static void validateJwtSecretValue(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable for production: APP_JWT_SECRET "
                            + "(generate with: openssl rand -base64 48)");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "Unsafe JWT secret: APP_JWT_SECRET must be at least " + MIN_SECRET_BYTES
                            + " bytes for HS256. Generate a stronger secret (openssl rand -base64 48).");
        }
        if (isForbiddenSecret(secret)) {
            throw new IllegalStateException(
                    "Unsafe JWT secret: APP_JWT_SECRET matches a known development default and must not be used. "
                            + "Generate a new secret (openssl rand -base64 48).");
        }
    }

    public static boolean isForbiddenSecret(String secret) {
        return FORBIDDEN_SECRETS.contains(secret);
    }

    public static boolean isProduction(Environment environment) {
        return hasAnyProfile(environment, PRODUCTION_PROFILES);
    }

    public static boolean isLocal(Environment environment) {
        return hasAnyProfile(environment, Set.of("local", "dev", "development"));
    }

    private static boolean hasAnyProfile(Environment environment, Set<String> candidates) {
        for (String profile : environment.getActiveProfiles()) {
            if (candidates.contains(profile.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static void requirePresent(ConfigurableEnvironment environment, String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable for production: " + name);
        }
    }

    @Override
    public int getOrder() {
        // Runs after configuration data (profiles and application*.properties) is loaded.
        return ConfigDataEnvironmentPostProcessor.ORDER + 10;
    }
}
