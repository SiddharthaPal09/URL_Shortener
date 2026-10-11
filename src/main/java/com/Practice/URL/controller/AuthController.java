package com.Practice.URL.controller;

import com.Practice.URL.dto.AuthResponse;
import com.Practice.URL.dto.LoginRequest;
import com.Practice.URL.dto.ProfileUpdateRequest;
import com.Practice.URL.dto.RegisterRequest;
import com.Practice.URL.dto.UserResponse;
import com.Practice.URL.security.CurrentUser;
import com.Practice.URL.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new account and receive a JWT")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Profile of the logged in user")
    public UserResponse getCurrentUser() {
        return authService.getCurrentUser(currentUser.email());
    }

    @PutMapping("/me")
    @Operation(summary = "Update name, email and/or password of the logged in user")
    public UserResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(currentUser.email(), request);
    }
}
