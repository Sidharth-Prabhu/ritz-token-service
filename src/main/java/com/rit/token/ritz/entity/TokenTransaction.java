package com.rit.token.ritz.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_transactions", indexes = {
    @Index(name = "idx_token_tx_user_time", columnList = "user_id, timestamp"),
    @Index(name = "idx_token_tx_ref", columnList = "reference_id")
})
public class TokenTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(length = 500)
    private String description;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public enum TransactionType {
        TOPUP,
        SPEND,
        REFUND,
        TRANSFER,
        ADJUSTMENT
    }

    public TokenTransaction() {
        this.timestamp = LocalDateTime.now();
    }

    public TokenTransaction(Long userId, String userName, String mobileNumber, BigDecimal amount,
                            TransactionType type, String description, String referenceId, BigDecimal balanceAfter) {
        this.userId = userId;
        this.userName = userName;
        this.mobileNumber = mobileNumber;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.referenceId = referenceId;
        this.balanceAfter = balanceAfter;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
