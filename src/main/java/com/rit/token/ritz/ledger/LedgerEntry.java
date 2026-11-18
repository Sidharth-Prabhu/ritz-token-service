package com.rit.token.ritz.ledger;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Strictly immutable record representing one leg of a double-entry financial transaction.
 * Never updated or deleted.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id", nullable = false, unique = true, length = 64)
    private String entryId;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "account_number", nullable = false, length = 64)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 16)
    private EntryType entryType;

    @Column(name = "amount_minor_units", nullable = false)
    private Long amountMinorUnits;

    @Column(name = "balance_after_minor_units", nullable = false)
    private Long balanceAfterMinorUnits;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LedgerEntry() {}

    public LedgerEntry(String entryId, String transactionId, String accountNumber, EntryType entryType, Long amountMinorUnits, Long balanceAfterMinorUnits, String description) {
        this.entryId = entryId;
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.entryType = entryType;
        this.amountMinorUnits = amountMinorUnits;
        this.balanceAfterMinorUnits = balanceAfterMinorUnits;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getEntryId() { return entryId; }
    public String getTransactionId() { return transactionId; }
    public String getAccountNumber() { return accountNumber; }
    public EntryType getEntryType() { return entryType; }
    public Long getAmountMinorUnits() { return amountMinorUnits; }
    public Long getBalanceAfterMinorUnits() { return balanceAfterMinorUnits; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
