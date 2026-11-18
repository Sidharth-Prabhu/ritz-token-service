package com.rit.token.ritz.refund;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_id", nullable = false, unique = true, length = 64)
    private String refundId;

    @Column(name = "original_transaction_id", nullable = false, length = 64)
    private String originalTransactionId;

    @Column(name = "student_id", nullable = false, length = 64)
    private String studentId;

    @Column(name = "merchant_id", length = 64)
    private String merchantId;

    @Column(name = "amount_minor_units", nullable = false)
    private Long amountMinorUnits;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RefundStatus status = RefundStatus.REQUESTED;

    @Column(name = "approved_by", length = 64)
    private String approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public RefundRequest() {}

    public RefundRequest(String refundId, String originalTransactionId, String studentId, String merchantId, Long amountMinorUnits, String reason) {
        this.refundId = refundId;
        this.originalTransactionId = originalTransactionId;
        this.studentId = studentId;
        this.merchantId = merchantId;
        this.amountMinorUnits = amountMinorUnits;
        this.reason = reason;
        this.status = RefundStatus.REQUESTED;
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

    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }

    public String getOriginalTransactionId() { return originalTransactionId; }
    public void setOriginalTransactionId(String originalTransactionId) { this.originalTransactionId = originalTransactionId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public Long getAmountMinorUnits() { return amountMinorUnits; }
    public void setAmountMinorUnits(Long amountMinorUnits) { this.amountMinorUnits = amountMinorUnits; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public RefundStatus getStatus() { return status; }
    public void setStatus(RefundStatus status) { this.status = status; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
