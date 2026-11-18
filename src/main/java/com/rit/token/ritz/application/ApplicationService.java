package com.rit.token.ritz.application;

import com.rit.token.ritz.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);
    private final RegisteredApplicationRepository applicationRepository;
    private final ApplicationPermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApplicationService(
            RegisteredApplicationRepository applicationRepository,
            ApplicationPermissionRepository permissionRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public static class AppCredentials {
        private String applicationId;
        private String apiKey;
        private String apiSecret;

        public AppCredentials(String applicationId, String apiKey, String apiSecret) {
            this.applicationId = applicationId;
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
        }

        public String getApplicationId() { return applicationId; }
        public String getApiKey() { return apiKey; }
        public String getApiSecret() { return apiSecret; }
    }

    @Transactional
    public AppCredentials registerApplication(String applicationId, String applicationName, String description, List<PermissionScope> scopes, String createdBy) {
        if (applicationRepository.existsByApplicationId(applicationId)) {
            throw new IllegalArgumentException("Application ID already exists: " + applicationId);
        }

        String apiKey = "rit_live_key_" + UUID.randomUUID().toString().replace("-", "");
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        String rawSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        String hashedSecret = passwordEncoder.encode(rawSecret);

        RegisteredApplication app = new RegisteredApplication(applicationId, applicationName, description, apiKey, hashedSecret, createdBy);
        applicationRepository.save(app);

        if (scopes != null) {
            for (PermissionScope scope : scopes) {
                permissionRepository.save(new ApplicationPermission(applicationId, scope));
            }
        }

        auditService.logAction(createdBy, "ADMIN", "REGISTER_APPLICATION", "APPLICATION", applicationId, null, "Created app with " + (scopes != null ? scopes.size() : 0) + " permissions");
        return new AppCredentials(applicationId, apiKey, rawSecret);
    }

    @Transactional
    public AppCredentials rotateCredentials(String applicationId, String actorId) {
        RegisteredApplication app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        String newApiKey = "rit_live_key_" + UUID.randomUUID().toString().replace("-", "");
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        String rawSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        app.setApiKey(newApiKey);
        app.setHashedApiSecret(passwordEncoder.encode(rawSecret));
        applicationRepository.save(app);

        auditService.logAction(actorId, "ADMIN", "ROTATE_APP_CREDENTIALS", "APPLICATION", applicationId, null, "Rotated API keys for " + applicationId);
        return new AppCredentials(applicationId, newApiKey, rawSecret);
    }

    @Transactional
    public void revokeCredentials(String applicationId, String actorId) {
        RegisteredApplication app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        app.setStatus("REVOKED");
        applicationRepository.save(app);
        auditService.logAction(actorId, "ADMIN", "REVOKE_APP_CREDENTIALS", "APPLICATION", applicationId, null, "Revoked application credentials");
    }

    public Optional<RegisteredApplication> validateCredentials(String apiKey, String rawSecret) {
        Optional<RegisteredApplication> appOpt = applicationRepository.findByApiKey(apiKey);
        if (appOpt.isEmpty()) return Optional.empty();

        RegisteredApplication app = appOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(app.getStatus())) return Optional.empty();

        if (rawSecret != null && !passwordEncoder.matches(rawSecret, app.getHashedApiSecret())) {
            return Optional.empty();
        }

        app.setLastRequestAt(LocalDateTime.now());
        applicationRepository.save(app);
        return Optional.of(app);
    }

    public boolean hasPermission(String applicationId, PermissionScope scope) {
        return permissionRepository.existsByApplicationIdAndPermissionScope(applicationId, scope);
    }

    public List<RegisteredApplication> getAllApplications() {
        return applicationRepository.findAll();
    }

    public Optional<RegisteredApplication> getApplication(String applicationId) {
        return applicationRepository.findByApplicationId(applicationId);
    }

    public List<ApplicationPermission> getPermissions(String applicationId) {
        return permissionRepository.findByApplicationId(applicationId);
    }
}
