package com.Practice.URL.service;

import com.Practice.URL.entity.User;
import com.Practice.URL.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevUserSeedingTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private DevCurrentUser seeder(boolean enabled, String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        if (profiles.length > 0) {
            environment.setActiveProfiles(profiles);
        }
        return new DevCurrentUser(userRepository, passwordEncoder, "dev@example.com", enabled, environment);
    }

    @Test
    void seedingNeverRunsInProductionProfileEvenWhenPropertyIsTrue() {
        DevCurrentUser devUser = seeder(true, "prod");
        devUser.createDefaultUser();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void seedingIsSkippedWhenDisabled() {
        DevCurrentUser devUser = seeder(false, "local");
        devUser.createDefaultUser();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void seedingRunsInDevelopmentWhenEnabled() {
        when(userRepository.existsByEmail("dev@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DevCurrentUser devUser = seeder(true, "local");
        devUser.createDefaultUser();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void seededPasswordIsRandomizedPerInstallationAndBcryptHashed() {
        when(userRepository.existsByEmail("dev@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            // Password must be a BCrypt hash, never a fixed literal from source.
            org.junit.jupiter.api.Assertions.assertTrue(
                    saved.getPassword().startsWith("$2"), "password must be BCrypt hashed");
            org.junit.jupiter.api.Assertions.assertNotEquals("temporary-password", saved.getPassword());
            return saved;
        });

        seeder(true, "local").createDefaultUser();
        verify(userRepository).save(any(User.class));

        // A second installation seeds a different password (random per run).
        when(userRepository.existsByEmail("dev@example.com")).thenReturn(false);
        final String[] first = new String[1];
        final String[] second = new String[1];
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            if (first[0] == null) {
                first[0] = saved.getPassword();
            } else {
                second[0] = saved.getPassword();
            }
            return saved;
        });
        seeder(true, "local").createDefaultUser();
        seeder(true, "local").createDefaultUser();
        assertNotEquals(first[0], second[0]);
    }
}
