package com.rit.token.ritz.application;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"application_id", "permission_scope"})
})
public class ApplicationPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, length = 64)
    private String applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_scope", nullable = false, length = 64)
    private PermissionScope permissionScope;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ApplicationPermission() {}

    public ApplicationPermission(String applicationId, PermissionScope permissionScope) {
        this.applicationId = applicationId;
        this.permissionScope = permissionScope;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getApplicationId() { return applicationId; }
    public PermissionScope getPermissionScope() { return permissionScope; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
