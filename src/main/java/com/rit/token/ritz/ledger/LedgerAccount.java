package com.rit.token.ritz.ledger;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_accounts")
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 64)
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 128)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 32)
    private AccountType accountType;

    @Column(name = "balance_minor_units", nullable = false)
    private Long balanceMinorUnits = 0L;

    @Column(nullable = false, length = 16)
    private String currency = "RITZ";

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LedgerAccount() {}

    public LedgerAccount(String accountNumber, String accountName, AccountType accountType) {
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.accountType = accountType;
        this.balanceMinorUnits = 0L;
        this.currency = "RITZ";
        this.status = "ACTIVE";
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

    /**
     * Standard accounting equation updates:
     * Assets & Expenses: Debit (+), Credit (-)
     * Liabilities, Equity & Revenue: Credit (+), Debit (-)
     */
    public void applyDebit(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (accountType == AccountType.ASSET || accountType == AccountType.EXPENSE) {
            this.balanceMinorUnits += amount;
        } else {
            this.balanceMinorUnits -= amount;
        }
    }

    public void applyCredit(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        if (accountType == AccountType.LIABILITY || accountType == AccountType.EQUITY || accountType == AccountType.REVENUE) {
            this.balanceMinorUnits += amount;
        } else {
            this.balanceMinorUnits -= amount;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public Long getBalanceMinorUnits() { return balanceMinorUnits; }
    public void setBalanceMinorUnits(Long balanceMinorUnits) { this.balanceMinorUnits = balanceMinorUnits; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
