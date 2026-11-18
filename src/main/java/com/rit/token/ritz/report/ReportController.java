package com.rit.token.ritz.report;

import com.rit.token.ritz.ledger.LedgerEntryRepository;
import com.rit.token.ritz.merchant.MerchantAccountRepository;
import com.rit.token.ritz.merchant.MerchantRepository;
import com.rit.token.ritz.reconciliation.ReconciliationService;
import com.rit.token.ritz.transaction.TransactionStatus;
import com.rit.token.ritz.transaction.WalletTransactionRepository;
import com.rit.token.ritz.wallet.StudentRepository;
import com.rit.token.ritz.wallet.WalletRepository;
import com.rit.token.ritz.wallet.WalletStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reports & Reconciliation", description = "Financial statistics, CSV exports and reconciliation between recharge, gateway and ledger")
public class ReportController {

    private final ReportService reportService;
    private final ReconciliationService reconciliationService;
    private final StudentRepository studentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantAccountRepository merchantAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public ReportController(
            ReportService reportService,
            ReconciliationService reconciliationService,
            StudentRepository studentRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository transactionRepository,
            MerchantRepository merchantRepository,
            MerchantAccountRepository merchantAccountRepository,
            LedgerEntryRepository ledgerEntryRepository) {
        this.reportService = reportService;
        this.reconciliationService = reconciliationService;
        this.studentRepository = studentRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.merchantRepository = merchantRepository;
        this.merchantAccountRepository = merchantAccountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @GetMapping("/reports/summary")
    @Operation(summary = "Get live system-wide executive financial summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        long totalStudents = studentRepository.count();
        long activeWallets = walletRepository.countByStatus(WalletStatus.ACTIVE);
        Long totalLiabilities = walletRepository.getTotalCirculationMinorUnits();
        Long totalRecharge = transactionRepository.getTotalRechargeVolume();
        Long totalSpending = transactionRepository.getTotalSpendingVolume();
        long totalTransactions = transactionRepository.count();
        long failedTransactions = transactionRepository.countByStatus(TransactionStatus.FAILED);
        long pendingTransactions = transactionRepository.countByStatus(TransactionStatus.PENDING);
        long activeMerchants = merchantRepository.count();
        Long pendingSettlements = merchantAccountRepository.getTotalPendingSettlements();

        Long totalDebits = ledgerEntryRepository.getTotalDebitVolume();
        Long totalCredits = ledgerEntryRepository.getTotalCreditVolume();
        boolean ledgerBalanced = totalDebits.equals(totalCredits);

        Map<String, Object> summary = new HashMap<>();
        summary.put("total_students", totalStudents);
        summary.put("active_wallets", activeWallets);
        summary.put("total_wallet_liabilities", totalLiabilities != null ? totalLiabilities : 0L);
        summary.put("total_recharge_volume", totalRecharge != null ? totalRecharge : 0L);
        summary.put("total_spending_volume", totalSpending != null ? totalSpending : 0L);
        summary.put("total_transactions", totalTransactions);
        summary.put("failed_transactions", failedTransactions);
        summary.put("pending_transactions", pendingTransactions);
        summary.put("active_merchants", activeMerchants);
        summary.put("pending_settlements", pendingSettlements != null ? pendingSettlements : 0L);
        summary.put("ledger_balanced", ledgerBalanced);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/reconciliation/check")
    @Operation(summary = "Execute financial reconciliation (Orders vs Payments vs Ledger)")
    public ResponseEntity<ReconciliationService.ReconciliationReport> runReconciliation() {
        return ResponseEntity.ok(reconciliationService.performReconciliation());
    }

    @GetMapping("/reports/recharges/csv")
    @Operation(summary = "Download recharge orders CSV report")
    public ResponseEntity<byte[]> exportRechargesCsv() {
        String csv = reportService.generateRechargeReportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recharges_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/reports/transactions/csv")
    @Operation(summary = "Download transactions CSV report")
    public ResponseEntity<byte[]> exportTransactionsCsv() {
        String csv = reportService.generateTransactionReportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/reports/ledger/csv")
    @Operation(summary = "Download double-entry ledger entries CSV report")
    public ResponseEntity<byte[]> exportLedgerCsv() {
        String csv = reportService.generateLedgerReportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ledger_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/reports/refunds/csv")
    @Operation(summary = "Download refunds CSV report")
    public ResponseEntity<byte[]> exportRefundsCsv() {
        String csv = reportService.generateRefundReportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"refunds_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/reports/settlements/csv")
    @Operation(summary = "Download settlements CSV report")
    public ResponseEntity<byte[]> exportSettlementsCsv() {
        String csv = reportService.generateSettlementReportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"settlements_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}
