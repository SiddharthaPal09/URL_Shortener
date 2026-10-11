package com.Practice.URL.service;

import com.Practice.URL.dto.AuthResponse;
import com.Practice.URL.dto.LoginRequest;
import com.Practice.URL.dto.ProfileUpdateRequest;
import com.Practice.URL.dto.RegisterRequest;
import com.Practice.URL.dto.UserResponse;
import com.Practice.URL.entity.User;
import com.Practice.URL.exception.BadRequestException;
import com.Practice.URL.exception.ConflictException;
import com.Practice.URL.exception.ResourceNotFoundException;
import com.Practice.URL.repository.UserRepository;
import com.Practice.URL.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("An account with this email already exists");
        }

        String token = jwtService.generateToken(email);
        return new AuthResponse(token, jwtService.getExpirationMs(), toResponse(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationFailedException();
        }

        String token = jwtService.generateToken(email);
        return new AuthResponse(token, jwtService.getExpirationMs(), toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return toResponse(findUser(email));
    }

    @Transactional
    public UserResponse updateProfile(String currentEmail, ProfileUpdateRequest request) {
        User user = findUser(currentEmail);

        String newEmail = request.getEmail().trim().toLowerCase();
        if (!user.getEmail().equals(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("An account with this email already exists");
        }

        boolean passwordChanged = request.getNewPassword() != null
                && !request.getNewPassword().isBlank();

        if (passwordChanged) {
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new BadRequestException("New password and confirmation do not match");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        user.setName(request.getName().trim());
        user.setEmail(newEmail);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("An account with this email already exists");
        }

        return toResponse(user);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private static class AuthenticationFailedException extends AuthenticationException {
        private AuthenticationFailedException() {
            super(INVALID_CREDENTIALS);
        }
    }
}
