package com.rit.token.ritz.application;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class RegisteredApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, unique = true, length = 64)
    private String applicationId;

    @Column(name = "application_name", nullable = false, length = 128)
    private String applicationName;

    @Column(length = 255)
    private String description;

    @Column(name = "api_key", nullable = false, unique = true, length = 128)
    private String apiKey;

    @Column(name = "hashed_api_secret", nullable = false, length = 255)
    private String hashedApiSecret;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, REVOKED

    @Column(name = "rate_limit_per_minute", nullable = false)
    private Integer rateLimitPerMinute = 300;

    @Column(name = "webhook_url", length = 255)
    private String webhookUrl;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "last_request_at")
    private LocalDateTime lastRequestAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public RegisteredApplication() {}

    public RegisteredApplication(String applicationId, String applicationName, String description, String apiKey, String hashedApiSecret, String createdBy) {
        this.applicationId = applicationId;
        this.applicationName = applicationName;
        this.description = description;
        this.apiKey = apiKey;
        this.hashedApiSecret = hashedApiSecret;
        this.createdBy = createdBy;
        this.status = "ACTIVE";
        this.rateLimitPerMinute = 300;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getHashedApiSecret() { return hashedApiSecret; }
    public void setHashedApiSecret(String hashedApiSecret) { this.hashedApiSecret = hashedApiSecret; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(Integer rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getLastRequestAt() { return lastRequestAt; }
    public void setLastRequestAt(LocalDateTime lastRequestAt) { this.lastRequestAt = lastRequestAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
