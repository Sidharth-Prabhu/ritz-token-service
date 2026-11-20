package com.rit.token.ritz.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class AdminStatsResponse {

    @JsonProperty("total_circulation")
    private Long totalCirculation; // Active student wallet balances (liabilities)

    @JsonProperty("total_issued")
    private Long totalIssued; // Lifetime tokens minted

    @JsonProperty("total_spent")
    private Long totalSpent; // Lifetime tokens spent at merchants

    @JsonProperty("total_refunded")
    private Long totalRefunded;

    @JsonProperty("total_students")
    private long totalStudents;

    @JsonProperty("active_wallets")
    private long activeWallets;

    @JsonProperty("total_transactions")
    private long totalTransactions;

    @JsonProperty("failed_transactions")
    private long failedTransactions;

    @JsonProperty("pending_transactions")
    private long pendingTransactions;

    @JsonProperty("active_merchants")
    private long activeMerchants;

    @JsonProperty("pending_settlements")
    private Long pendingSettlements;

    @JsonProperty("ledger_total_debits")
    private Long ledgerTotalDebits;

    @JsonProperty("ledger_total_credits")
    private Long ledgerTotalCredits;

    @JsonProperty("ledger_balanced")
    private boolean ledgerBalanced;

    @JsonProperty("volume_last_24h")
    private Long volumeLast24h;

    @JsonProperty("issued_last_24h")
    private Long issuedLast24h;

    @JsonProperty("top_wallets")
    private List<Map<String, Object>> topWallets;

    @JsonProperty("merchant_summary")
    private List<Map<String, Object>> merchantSummary;

    public AdminStatsResponse() {}

    public Long getTotalCirculation() {
        return totalCirculation;
    }

    public void setTotalCirculation(Long totalCirculation) {
        this.totalCirculation = totalCirculation;
    }

    public Long getTotalIssued() {
        return totalIssued;
    }

    public void setTotalIssued(Long totalIssued) {
        this.totalIssued = totalIssued;
    }

    public Long getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(Long totalSpent) {
        this.totalSpent = totalSpent;
    }

    public Long getTotalRefunded() {
        return totalRefunded;
    }

    public void setTotalRefunded(Long totalRefunded) {
        this.totalRefunded = totalRefunded;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getActiveWallets() {
        return activeWallets;
    }

    public void setActiveWallets(long activeWallets) {
        this.activeWallets = activeWallets;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getFailedTransactions() {
        return failedTransactions;
    }

    public void setFailedTransactions(long failedTransactions) {
        this.failedTransactions = failedTransactions;
    }

    public long getPendingTransactions() {
        return pendingTransactions;
    }

    public void setPendingTransactions(long pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }

    public long getActiveMerchants() {
        return activeMerchants;
    }

    public void setActiveMerchants(long activeMerchants) {
        this.activeMerchants = activeMerchants;
    }

    public Long getPendingSettlements() {
        return pendingSettlements;
    }

    public void setPendingSettlements(Long pendingSettlements) {
        this.pendingSettlements = pendingSettlements;
    }

    public Long getLedgerTotalDebits() {
        return ledgerTotalDebits;
    }

    public void setLedgerTotalDebits(Long ledgerTotalDebits) {
        this.ledgerTotalDebits = ledgerTotalDebits;
    }

    public Long getLedgerTotalCredits() {
        return ledgerTotalCredits;
    }

    public void setLedgerTotalCredits(Long ledgerTotalCredits) {
        this.ledgerTotalCredits = ledgerTotalCredits;
    }

    public boolean isLedgerBalanced() {
        return ledgerBalanced;
    }

    public void setLedgerBalanced(boolean ledgerBalanced) {
        this.ledgerBalanced = ledgerBalanced;
    }

    public Long getVolumeLast24h() {
        return volumeLast24h;
    }

    public void setVolumeLast24h(Long volumeLast24h) {
        this.volumeLast24h = volumeLast24h;
    }

    public Long getIssuedLast24h() {
        return issuedLast24h;
    }

    public void setIssuedLast24h(Long issuedLast24h) {
        this.issuedLast24h = issuedLast24h;
    }

    public List<Map<String, Object>> getTopWallets() {
        return topWallets;
    }

    public void setTopWallets(List<Map<String, Object>> topWallets) {
        this.topWallets = topWallets;
    }

    public List<Map<String, Object>> getMerchantSummary() {
        return merchantSummary;
    }

    public void setMerchantSummary(List<Map<String, Object>> merchantSummary) {
        this.merchantSummary = merchantSummary;
    }
}
