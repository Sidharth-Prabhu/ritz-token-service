package com.rit.token.ritz.transaction;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"idempotency_key", "scope"})
})
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String scope;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "http_status", nullable = false)
    private Integer httpStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public IdempotencyRecord() {}

    public IdempotencyRecord(String idempotencyKey, String scope, String requestHash, Integer httpStatus, String responseBody) {
        this.idempotencyKey = idempotencyKey;
        this.scope = scope;
        this.requestHash = requestHash;
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getScope() { return scope; }
    public String getRequestHash() { return requestHash; }
    public Integer getHttpStatus() { return httpStatus; }
    public String getResponseBody() { return responseBody; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
