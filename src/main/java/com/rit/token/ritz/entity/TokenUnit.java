package com.rit.token.ritz.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_units", indexes = {
    @Index(name = "idx_token_units_owner_status", columnList = "owner_id, status"),
    @Index(name = "idx_token_units_hash", columnList = "token_hash")
})
public class TokenUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", unique = true, nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TokenStatus status = TokenStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "spent_at")
    private LocalDateTime spentAt;

    @Column(name = "order_ref", length = 100)
    private String orderRef;

    public enum TokenStatus {
        ACTIVE,
        SPENT,
        REVOKED
    }

    public TokenUnit() {
        this.createdAt = LocalDateTime.now();
        this.status = TokenStatus.ACTIVE;
    }

    public TokenUnit(String tokenHash, Long ownerId) {
        this();
        this.tokenHash = tokenHash;
        this.ownerId = ownerId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public TokenStatus getStatus() { return status; }
    public void setStatus(TokenStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSpentAt() { return spentAt; }
    public void setSpentAt(LocalDateTime spentAt) { this.spentAt = spentAt; }

    public String getOrderRef() { return orderRef; }
    public void setOrderRef(String orderRef) { this.orderRef = orderRef; }
}
