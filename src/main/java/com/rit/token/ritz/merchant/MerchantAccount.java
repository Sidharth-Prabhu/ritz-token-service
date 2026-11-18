package com.rit.token.ritz.merchant;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_accounts")
public class MerchantAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, unique = true, length = 64)
    private String merchantId;

    @Column(name = "total_earned_minor_units", nullable = false)
    private Long totalEarnedMinorUnits = 0L;

    @Column(name = "pending_settlement_minor_units", nullable = false)
    private Long pendingSettlementMinorUnits = 0L;

    @Column(name = "settled_minor_units", nullable = false)
    private Long settledMinorUnits = 0L;

    @Column(name = "last_settlement_at")
    private LocalDateTime lastSettlementAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MerchantAccount() {}

    public MerchantAccount(String merchantId) {
        this.merchantId = merchantId;
        this.totalEarnedMinorUnits = 0L;
        this.pendingSettlementMinorUnits = 0L;
        this.settledMinorUnits = 0L;
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

    public void creditEarnings(Long amount) {
        if (amount == null || amount <= 0) return;
        this.totalEarnedMinorUnits += amount;
        this.pendingSettlementMinorUnits += amount;
    }

    public void debitForRefund(Long amount) {
        if (amount == null || amount <= 0) return;
        this.totalEarnedMinorUnits -= amount;
        this.pendingSettlementMinorUnits -= amount;
    }

    public void completeSettlement(Long amount) {
        if (amount == null || amount <= 0) return;
        if (this.pendingSettlementMinorUnits < amount) {
            throw new IllegalStateException("Settlement amount exceeds pending settlement balance");
        }
        this.pendingSettlementMinorUnits -= amount;
        this.settledMinorUnits += amount;
        this.lastSettlementAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public Long getTotalEarnedMinorUnits() { return totalEarnedMinorUnits; }
    public void setTotalEarnedMinorUnits(Long totalEarnedMinorUnits) { this.totalEarnedMinorUnits = totalEarnedMinorUnits; }

    public Long getPendingSettlementMinorUnits() { return pendingSettlementMinorUnits; }
    public void setPendingSettlementMinorUnits(Long pendingSettlementMinorUnits) { this.pendingSettlementMinorUnits = pendingSettlementMinorUnits; }

    public Long getSettledMinorUnits() { return settledMinorUnits; }
    public void setSettledMinorUnits(Long settledMinorUnits) { this.settledMinorUnits = settledMinorUnits; }

    public LocalDateTime getLastSettlementAt() { return lastSettlementAt; }
    public void setLastSettlementAt(LocalDateTime lastSettlementAt) { this.lastSettlementAt = lastSettlementAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
