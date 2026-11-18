package com.rit.token.ritz.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false, length = 64)
    private String recipientId;

    @Column(name = "recipient_type", nullable = false, length = 32)
    private String recipientType; // STUDENT, ADMIN, MERCHANT

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(String recipientId, String recipientType, String title, String message, NotificationType type) {
        this.recipientId = recipientId;
        this.recipientType = recipientType;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getRecipientId() { return recipientId; }
    public String getRecipientType() { return recipientType; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
