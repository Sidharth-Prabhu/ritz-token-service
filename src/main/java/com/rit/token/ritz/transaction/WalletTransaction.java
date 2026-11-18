package com.rit.token.ritz.transaction;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "source_account", nullable = false, length = 64)
    private String sourceAccount;

    @Column(name = "destination_account", nullable = false, length = 64)
    private String destinationAccount;

    @Column(name = "amount_minor_units", nullable = false)
    private Long amountMinorUnits;

    @Column(nullable = false, length = 16)
    private String currency = "RITZ";

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "initiating_application_id", length = 64)
    private String initiatingApplicationId;

    @Column(name = "merchant_id", length = 64)
    private String merchantId;

    @Column(name = "student_id", length = 64)
    private String studentId;

    @Column(length = 255)
    private String description;

    @Column(name = "previous_balance_minor_units")
    private Long previousBalanceMinorUnits;

    @Column(name = "current_balance_minor_units")
    private Long currentBalanceMinorUnits;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WalletTransaction() {}

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

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getSourceAccount() { return sourceAccount; }
    public void setSourceAccount(String sourceAccount) { this.sourceAccount = sourceAccount; }

    public String getDestinationAccount() { return destinationAccount; }
    public void setDestinationAccount(String destinationAccount) { this.destinationAccount = destinationAccount; }

    public Long getAmountMinorUnits() { return amountMinorUnits; }
    public void setAmountMinorUnits(Long amountMinorUnits) { this.amountMinorUnits = amountMinorUnits; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getInitiatingApplicationId() { return initiatingApplicationId; }
    public void setInitiatingApplicationId(String initiatingApplicationId) { this.initiatingApplicationId = initiatingApplicationId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getPreviousBalanceMinorUnits() { return previousBalanceMinorUnits; }
    public void setPreviousBalanceMinorUnits(Long previousBalanceMinorUnits) { this.previousBalanceMinorUnits = previousBalanceMinorUnits; }

    public Long getCurrentBalanceMinorUnits() { return currentBalanceMinorUnits; }
    public void setCurrentBalanceMinorUnits(Long currentBalanceMinorUnits) { this.currentBalanceMinorUnits = currentBalanceMinorUnits; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
