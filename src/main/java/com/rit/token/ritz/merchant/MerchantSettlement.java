package com.rit.token.ritz.merchant;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
public class MerchantSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false, unique = true, length = 64)
    private String settlementId;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Column(name = "amount_minor_units", nullable = false)
    private Long amountMinorUnits;

    @Column(nullable = false, length = 32)
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "bank_reference", length = 128)
    private String bankReference;

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MerchantSettlement() {}

    public MerchantSettlement(String settlementId, String merchantId, Long amountMinorUnits, String notes) {
        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.amountMinorUnits = amountMinorUnits;
        this.notes = notes;
        this.status = "PENDING";
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

    public String getSettlementId() { return settlementId; }
    public void setSettlementId(String settlementId) { this.settlementId = settlementId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public Long getAmountMinorUnits() { return amountMinorUnits; }
    public void setAmountMinorUnits(Long amountMinorUnits) { this.amountMinorUnits = amountMinorUnits; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBankReference() { return bankReference; }
    public void setBankReference(String bankReference) { this.bankReference = bankReference; }

    public LocalDateTime getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDateTime settlementDate) { this.settlementDate = settlementDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
