package com.rit.token.ritz.ledger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ledger")
@Tag(name = "Double-Entry Ledger", description = "Immutable double-entry general ledger inspection and balancing verification")
public class LedgerController {

    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;
    private final LedgerService ledgerService;

    public LedgerController(
            LedgerAccountRepository accountRepository,
            LedgerEntryRepository entryRepository,
            LedgerService ledgerService) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
        this.ledgerService = ledgerService;
    }

    @GetMapping("/accounts")
    @Operation(summary = "List all double-entry ledger accounts with current balances")
    public ResponseEntity<List<LedgerAccount>> getAccounts() {
        return ResponseEntity.ok(accountRepository.findAll());
    }

    @GetMapping("/entries")
    @Operation(summary = "Get immutable double-entry ledger journal entries")
    public ResponseEntity<Page<LedgerEntry>> getEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String accountNumber) {

        if (accountNumber != null && !accountNumber.isBlank()) {
            return ResponseEntity.ok(entryRepository.findByAccountNumberOrderByCreatedAtDesc(
                    accountNumber,
                    PageRequest.of(page, size, Sort.by("createdAt").descending())
            ));
        }

        return ResponseEntity.ok(entryRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        ));
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify system-wide double-entry ledger balance (Debits == Credits)")
    public ResponseEntity<Map<String, Object>> verifyBalance() {
        Long totalDebits = entryRepository.getTotalDebitVolume();
        Long totalCredits = entryRepository.getTotalCreditVolume();
        boolean isBalanced = ledgerService.verifyLedgerBalance();

        Map<String, Object> result = new HashMap<>();
        result.put("is_balanced", isBalanced);
        result.put("total_debits", totalDebits);
        result.put("total_credits", totalCredits);
        result.put("discrepancy", totalDebits - totalCredits);
        result.put("status", isBalanced ? "BALANCED" : "DISCREPANCY_DETECTED");

        return ResponseEntity.ok(result);
    }
}
