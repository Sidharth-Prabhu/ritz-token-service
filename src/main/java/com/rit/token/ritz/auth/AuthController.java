package com.rit.token.ritz.auth;

import com.rit.token.ritz.auth.dto.AuthResponse;
import com.rit.token.ritz.auth.dto.LoginRequest;
import com.rit.token.ritz.auth.dto.PhoneAuthRequest;
import com.rit.token.ritz.auth.dto.RegisterRequest;
import com.rit.token.ritz.auth.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Student, Merchant, Admin and Developer Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/phone")
    @Operation(summary = "Authenticate or auto-provision student by phone number (SSO / Seamless Portal Access)")
    public ResponseEntity<AuthResponse> loginByPhone(@Valid @RequestBody PhoneAuthRequest request) {
        return ResponseEntity.ok(authService.authenticateOrRegisterByPhone(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user (Student, Merchant, or Developer)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "User login with username, email, or student ID")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        String username;
        if (authentication.getPrincipal() instanceof User user) {
            username = user.getUsername();
        } else {
            username = authentication.getName();
        }
        return ResponseEntity.ok(authService.getProfile(username));
    }
}
