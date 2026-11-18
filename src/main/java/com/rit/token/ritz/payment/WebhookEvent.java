package com.rit.token.ritz.payment;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 128)
    private String eventId;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    @Column(nullable = false, length = 32)
    private String status; // RECEIVED, PROCESSED, FAILED, DUPLICATE

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WebhookEvent() {}

    public WebhookEvent(String eventId, String provider, String eventType, String payloadHash, String status) {
        this.eventId = eventId;
        this.provider = provider;
        this.eventType = eventType;
        this.payloadHash = payloadHash;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getProvider() { return provider; }
    public String getEventType() { return eventType; }
    public String getPayloadHash() { return payloadHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
