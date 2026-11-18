package com.rit.token.ritz.application;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Application Management", description = "Register, configure and manage API credentials for integrated campus applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final RegisteredApplicationRepository applicationRepository;

    public ApplicationController(ApplicationService applicationService, RegisteredApplicationRepository applicationRepository) {
        this.applicationService = applicationService;
        this.applicationRepository = applicationRepository;
    }

    @PostMapping
    @Operation(summary = "Register a new external campus application")
    public ResponseEntity<Map<String, Object>> registerApplication(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        String appId = (String) request.get("application_id");
        String appName = (String) request.get("application_name");
        String description = (String) request.get("description");
        List<?> scopesRaw = (List<?>) request.get("scopes");

        List<PermissionScope> scopes = scopesRaw != null ?
                scopesRaw.stream().map(s -> PermissionScope.valueOf(s.toString().toUpperCase())).collect(Collectors.toList())
                : List.of(PermissionScope.TRANSACTION_DEBIT, PermissionScope.BALANCE_READ, PermissionScope.TRANSACTION_VALIDATE);

        String createdBy = authentication != null ? authentication.getName() : "ADMIN";
        ApplicationService.AppCredentials credentials = applicationService.registerApplication(appId, appName, description, scopes, createdBy);

        Map<String, Object> response = new HashMap<>();
        response.put("application_id", credentials.getApplicationId());
        response.put("api_key", credentials.getApiKey());
        response.put("api_secret", credentials.getApiSecret());
        response.put("warning", "Save this API secret now. It is hashed in the database and cannot be retrieved later.");

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all registered applications")
    public ResponseEntity<List<RegisteredApplication>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/{applicationId}")
    @Operation(summary = "Get application details and permissions")
    public ResponseEntity<Map<String, Object>> getApplication(@PathVariable String applicationId) {
        RegisteredApplication app = applicationService.getApplication(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        List<ApplicationPermission> permissions = applicationService.getPermissions(applicationId);

        Map<String, Object> response = new HashMap<>();
        response.put("application", app);
        response.put("permissions", permissions.stream().map(ApplicationPermission::getPermissionScope).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{applicationId}")
    @Operation(summary = "Update application details (status, rate limit, webhook)")
    public ResponseEntity<RegisteredApplication> updateApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> updates) {
        RegisteredApplication app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        if (updates.containsKey("application_name")) app.setApplicationName((String) updates.get("application_name"));
        if (updates.containsKey("description")) app.setDescription((String) updates.get("description"));
        if (updates.containsKey("status")) app.setStatus((String) updates.get("status"));
        if (updates.containsKey("webhook_url")) app.setWebhookUrl((String) updates.get("webhook_url"));
        if (updates.containsKey("rate_limit_per_minute")) {
            app.setRateLimitPerMinute(Integer.parseInt(updates.get("rate_limit_per_minute").toString()));
        }

        return ResponseEntity.ok(applicationRepository.save(app));
    }

    @PostMapping("/{applicationId}/rotate-credentials")
    @Operation(summary = "Rotate API key and secret for an application")
    public ResponseEntity<Map<String, Object>> rotateCredentials(
            @PathVariable String applicationId,
            Authentication authentication) {
        String actorId = authentication != null ? authentication.getName() : "ADMIN";
        ApplicationService.AppCredentials credentials = applicationService.rotateCredentials(applicationId, actorId);

        Map<String, Object> response = new HashMap<>();
        response.put("application_id", credentials.getApplicationId());
        response.put("api_key", credentials.getApiKey());
        response.put("api_secret", credentials.getApiSecret());
        response.put("message", "Credentials rotated successfully. Previous credentials have been invalidated.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{applicationId}/revoke-credentials")
    @Operation(summary = "Revoke credentials and deactivate application access")
    public ResponseEntity<Map<String, String>> revokeCredentials(
            @PathVariable String applicationId,
            Authentication authentication) {
        String actorId = authentication != null ? authentication.getName() : "ADMIN";
        applicationService.revokeCredentials(applicationId, actorId);
        return ResponseEntity.ok(Map.of("message", "Application access revoked successfully."));
    }
}
