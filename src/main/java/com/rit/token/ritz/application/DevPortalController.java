package com.rit.token.ritz.application;

import com.rit.token.ritz.application.dto.DevPortalAuthRequest;
import com.rit.token.ritz.auth.JwtService;
import com.rit.token.ritz.auth.Role;
import com.rit.token.ritz.auth.User;
import com.rit.token.ritz.auth.UserRepository;
import com.rit.token.ritz.config.system.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dev-portal")
@Tag(name = "Developer Portal Auth", description = "Access and session management for the Developer Portal")
public class DevPortalController {

    private final SystemConfigService systemConfigService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${dev.portal.password:RitzDev2026!}")
    private String defaultDevPassword;

    public DevPortalController(
            SystemConfigService systemConfigService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserRepository userRepository) {
        this.systemConfigService = systemConfigService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate developer with portal password")
    public ResponseEntity<Map<String, Object>> login(@RequestBody DevPortalAuthRequest request) {
        if (request == null || request.getPassword() == null || request.getPassword().isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Password is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }

        String inputPassword = request.getPassword().trim();
        String currentPassword = systemConfigService.getConfig("DEV_PORTAL_PASSWORD", defaultDevPassword);

        boolean matches = inputPassword.equals(currentPassword) ||
                (currentPassword.startsWith("$2a$") && passwordEncoder.matches(inputPassword, currentPassword));

        if (!matches) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Invalid developer portal password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        // Retrieve or create platform developer principal
        User devUser = userRepository.findByUsername("developer")
                .orElseGet(() -> {
                    User newUser = new User("developer", passwordEncoder.encode(defaultDevPassword), "Platform Developer", "dev@ritchennai.edu.in", Role.APP_DEVELOPER);
                    return userRepository.save(newUser);
                });

        String token = jwtService.generateToken(devUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("role", devUser.getRole().name());
        response.put("username", devUser.getUsername());
        response.put("message", "Developer portal unlocked successfully");

        return ResponseEntity.ok(response);
    }
}
