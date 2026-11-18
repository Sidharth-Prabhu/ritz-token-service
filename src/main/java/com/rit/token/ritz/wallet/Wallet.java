package com.rit.token.ritz.wallet;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false, unique = true, length = 64)
    private String walletId;

    @Column(name = "student_id", nullable = false, unique = true, length = 64)
    private String studentId;

    @Column(name = "balance_minor_units", nullable = false)
    private Long balanceMinorUnits = 0L;

    @Column(nullable = false, length = 16)
    private String currency = "RITZ";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WalletStatus status = WalletStatus.ACTIVE;

    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Wallet() {}

    public Wallet(String walletId, String studentId) {
        this.walletId = walletId;
        this.studentId = studentId;
        this.balanceMinorUnits = 0L;
        this.status = WalletStatus.ACTIVE;
        this.currency = "RITZ";
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

    public boolean canDebit(Long amount) {
        if (amount == null || amount <= 0) return false;
        return this.status == WalletStatus.ACTIVE && this.balanceMinorUnits >= amount;
    }

    public void debit(Long amount) {
        if (!canDebit(amount)) {
            throw new IllegalStateException("Insufficient funds or wallet inactive");
        }
        this.balanceMinorUnits -= amount;
        this.lastTransactionAt = LocalDateTime.now();
    }

    public void credit(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        if (this.status == WalletStatus.BLOCKED || this.status == WalletStatus.CLOSED) {
            throw new IllegalStateException("Cannot credit to a blocked or closed wallet");
        }
        this.balanceMinorUnits += amount;
        this.lastTransactionAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Long getBalanceMinorUnits() { return balanceMinorUnits; }
    public void setBalanceMinorUnits(Long balanceMinorUnits) { this.balanceMinorUnits = balanceMinorUnits; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public WalletStatus getStatus() { return status; }
    public void setStatus(WalletStatus status) { this.status = status; }

    public LocalDateTime getLastTransactionAt() { return lastTransactionAt; }
    public void setLastTransactionAt(LocalDateTime lastTransactionAt) { this.lastTransactionAt = lastTransactionAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
