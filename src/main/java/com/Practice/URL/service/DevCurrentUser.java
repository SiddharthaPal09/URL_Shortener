package com.Practice.URL.service;

import com.Practice.URL.config.ProductionEnvironmentValidator;
import com.Practice.URL.entity.User;
import com.Practice.URL.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Optional development-only helper that seeds a local test account.
 * The seeding is disabled by default in production: both the
 * {@code app.dev-user.enabled} property and an explicit production profile
 * check must allow it, and the prod configuration file forces the flag off.
 */
@Component
public class DevCurrentUser {

    public static final String DEFAULT_NAME = "Development User";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final boolean enabled;
    private final Environment environment;

    public DevCurrentUser(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.dev-user.email}") String email,
                          @Value("${app.dev-user.enabled:true}") boolean enabled,
                          Environment environment) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.enabled = enabled;
        this.environment = environment;
    }

    public String email() {
        return email;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createDefaultUser() {
        if (!enabled) {
            return;
        }
        if (ProductionEnvironmentValidator.isProduction(environment)) {
            return;
        }
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = new User();
        user.setName(DEFAULT_NAME);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(generateDevPassword()));
        user.setRole("USER");
        userRepository.save(user);
    }

    /**
     * The development account gets a random password per installation instead of a
     * fixed string baked into the source code. It is never logged.
     */
    private static String generateDevPassword() {
        byte[] raw = new byte[18];
        new java.security.SecureRandom().nextBytes(raw);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}
