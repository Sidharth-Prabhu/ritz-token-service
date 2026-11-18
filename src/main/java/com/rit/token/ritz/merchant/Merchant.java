package com.rit.token.ritz.merchant;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, unique = true, length = 64)
    private String merchantId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MerchantCategory category;

    @Column(name = "associated_application_id", length = 64)
    private String associatedApplicationId;

    @Column(name = "contact_email", length = 128)
    private String contactEmail;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, INACTIVE

    @Column(name = "settlement_account_number", length = 64)
    private String settlementAccountNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Merchant() {}

    public Merchant(String merchantId, String name, String description, MerchantCategory category, String associatedApplicationId, String contactEmail, String contactPhone) {
        this.merchantId = merchantId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.associatedApplicationId = associatedApplicationId;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.status = "ACTIVE";
        this.settlementAccountNumber = "LIAB-MERCH-" + merchantId;
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

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public MerchantCategory getCategory() { return category; }
    public void setCategory(MerchantCategory category) { this.category = category; }

    public String getAssociatedApplicationId() { return associatedApplicationId; }
    public void setAssociatedApplicationId(String associatedApplicationId) { this.associatedApplicationId = associatedApplicationId; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSettlementAccountNumber() { return settlementAccountNumber; }
    public void setSettlementAccountNumber(String settlementAccountNumber) { this.settlementAccountNumber = settlementAccountNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
