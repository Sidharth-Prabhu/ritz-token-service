package com.rit.token.ritz.reconciliation;

import com.rit.token.ritz.ledger.EntryType;
import com.rit.token.ritz.ledger.LedgerEntry;
import com.rit.token.ritz.ledger.LedgerEntryRepository;
import com.rit.token.ritz.payment.*;
import com.rit.token.ritz.transaction.TransactionStatus;
import com.rit.token.ritz.transaction.TransactionType;
import com.rit.token.ritz.transaction.WalletTransaction;
import com.rit.token.ritz.transaction.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final RechargeOrderRepository rechargeOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final WalletTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public ReconciliationService(
            RechargeOrderRepository rechargeOrderRepository,
            PaymentRecordRepository paymentRecordRepository,
            WalletTransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository) {
        this.rechargeOrderRepository = rechargeOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public static class ReconciliationDiscrepancy {
        private String orderId;
        private String paymentId;
        private String studentId;
        private String discrepancyType;
        private String details;
        private LocalDateTime detectedAt;

        public ReconciliationDiscrepancy(String orderId, String paymentId, String studentId, String discrepancyType, String details) {
            this.orderId = orderId;
            this.paymentId = paymentId;
            this.studentId = studentId;
            this.discrepancyType = discrepancyType;
            this.details = details;
            this.detectedAt = LocalDateTime.now();
        }

        public String getOrderId() { return orderId; }
        public String getPaymentId() { return paymentId; }
        public String getStudentId() { return studentId; }
        public String getDiscrepancyType() { return discrepancyType; }
        public String getDetails() { return details; }
        public LocalDateTime getDetectedAt() { return detectedAt; }
    }

    public static class ReconciliationReport {
        private long totalOrdersChecked;
        private long matchedRecords;
        private long discrepanciesCount;
        private boolean ledgerGloballyBalanced;
        private List<ReconciliationDiscrepancy> discrepancies;
        private LocalDateTime generatedAt;

        public ReconciliationReport(long totalOrdersChecked, long matchedRecords, long discrepanciesCount, boolean ledgerGloballyBalanced, List<ReconciliationDiscrepancy> discrepancies) {
            this.totalOrdersChecked = totalOrdersChecked;
            this.matchedRecords = matchedRecords;
            this.discrepanciesCount = discrepanciesCount;
            this.ledgerGloballyBalanced = ledgerGloballyBalanced;
            this.discrepancies = discrepancies;
            this.generatedAt = LocalDateTime.now();
        }

        public long getTotalOrdersChecked() { return totalOrdersChecked; }
        public long getMatchedRecords() { return matchedRecords; }
        public long getDiscrepanciesCount() { return discrepanciesCount; }
        public boolean isLedgerGloballyBalanced() { return ledgerGloballyBalanced; }
        public List<ReconciliationDiscrepancy> getDiscrepancies() { return discrepancies; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
    }

    public ReconciliationReport performReconciliation() {
        List<RechargeOrder> paidOrders = rechargeOrderRepository.findAll().stream()
                .filter(o -> o.getStatus() == PaymentOrderStatus.PAID)
                .toList();

        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();
        long matched = 0;

        for (RechargeOrder order : paidOrders) {
            boolean hasIssue = false;

            // 1. Check payment record exists
            Optional<PaymentRecord> paymentRecord = paymentRecordRepository.findByOrderId(order.getOrderId());
            if (paymentRecord.isEmpty() && order.getPaymentId() != null) {
                paymentRecord = paymentRecordRepository.findByPaymentId(order.getPaymentId());
            }

            if (paymentRecord.isEmpty()) {
                discrepancies.add(new ReconciliationDiscrepancy(
                        order.getOrderId(),
                        order.getPaymentId(),
                        order.getStudentId(),
                        "MISSING_PAYMENT_RECORD",
                        "Order marked PAID but no corresponding PaymentRecord in database"
                ));
                hasIssue = true;
            }

            // 2. Check wallet transaction exists
            Optional<WalletTransaction> txnOpt = transactionRepository.findByReferenceId(order.getOrderId());
            if (txnOpt.isEmpty()) {
                discrepancies.add(new ReconciliationDiscrepancy(
                        order.getOrderId(),
                        order.getPaymentId(),
                        order.getStudentId(),
                        "MISSING_WALLET_TRANSACTION",
                        "Order marked PAID but no matching WalletTransaction found"
                ));
                hasIssue = true;
            } else {
                WalletTransaction txn = txnOpt.get();
                if (txn.getStatus() != TransactionStatus.COMPLETED) {
                    discrepancies.add(new ReconciliationDiscrepancy(
                            order.getOrderId(),
                            order.getPaymentId(),
                            order.getStudentId(),
                            "TRANSACTION_STATUS_MISMATCH",
                            "Recharge transaction status is " + txn.getStatus() + " instead of COMPLETED"
                    ));
                    hasIssue = true;
                }
                if (!txn.getAmountMinorUnits().equals(order.getTokenAmount())) {
                    discrepancies.add(new ReconciliationDiscrepancy(
                            order.getOrderId(),
                            order.getPaymentId(),
                            order.getStudentId(),
                            "AMOUNT_MISMATCH",
                            "Order token amount (" + order.getTokenAmount() + ") != Transaction amount (" + txn.getAmountMinorUnits() + ")"
                    ));
                    hasIssue = true;
                }

                // 3. Check double-entry ledger entries exist for this transaction
                List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionIdOrderByCreatedAtAsc(txn.getTransactionId());
                if (entries.size() < 2) {
                    discrepancies.add(new ReconciliationDiscrepancy(
                            order.getOrderId(),
                            order.getPaymentId(),
                            order.getStudentId(),
                            "INCOMPLETE_LEDGER_ENTRIES",
                            "Transaction " + txn.getTransactionId() + " has " + entries.size() + " ledger entries (expected >= 2)"
                    ));
                    hasIssue = true;
                }
            }

            if (!hasIssue) {
                matched++;
            }
        }

        // Check global double-entry equality
        Long totalDebits = ledgerEntryRepository.getTotalDebitVolume();
        Long totalCredits = ledgerEntryRepository.getTotalCreditVolume();
        boolean globallyBalanced = totalDebits.equals(totalCredits);

        log.info("Reconciliation complete: Checked={}, Matched={}, Discrepancies={}, LedgerBalanced={}",
                paidOrders.size(), matched, discrepancies.size(), globallyBalanced);

        return new ReconciliationReport(paidOrders.size(), matched, discrepancies.size(), globallyBalanced, discrepancies);
    }
}
