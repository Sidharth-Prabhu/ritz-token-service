package com.rit.token.ritz.admin;

import com.rit.token.ritz.admin.dto.AdminDepositRequest;
import com.rit.token.ritz.admin.dto.AdminDepositResponse;
import com.rit.token.ritz.admin.dto.AdminStatsResponse;
import com.rit.token.ritz.admin.dto.StudentSearchResult;
import com.rit.token.ritz.auth.User;
import com.rit.token.ritz.transaction.TransactionStatus;
import com.rit.token.ritz.transaction.TransactionType;
import com.rit.token.ritz.transaction.WalletTransaction;
import com.rit.token.ritz.wallet.Wallet;
import com.rit.token.ritz.wallet.WalletRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Management Portal", description = "Endpoints for institutional monitoring, transaction surveillance and direct deposit")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FINANCE_ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final WalletRepository walletRepository;

    public AdminController(AdminService adminService, WalletRepository walletRepository) {
        this.adminService = adminService;
        this.walletRepository = walletRepository;
    }

    private String getActorName(Authentication auth) {
        if (auth == null) return "ADMIN";
        if (auth.getPrincipal() instanceof User u) return u.getUsername();
        return auth.getName();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get comprehensive RITZ token circulation metrics and ledger status")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getCirculationStats());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get surveillance feed of recent transactions with limits and filters")
    public ResponseEntity<Page<WalletTransaction>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminService.getRecentTransactions(page, limit, type, status, search));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Direct deposit tokens to student wallet with double-entry accounting")
    public ResponseEntity<AdminDepositResponse> directDeposit(
            @Valid @RequestBody AdminDepositRequest request,
            Authentication authentication) {
        String actor = getActorName(authentication);
        AdminDepositResponse response = adminService.executeDirectDeposit(request, actor);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/students/search")
    @Operation(summary = "Search students by student ID, phone number or name for deposit lookup")
    public ResponseEntity<List<StudentSearchResult>> searchStudents(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "15") int limit) {
        return ResponseEntity.ok(adminService.searchStudents(q, limit));
    }

    @GetMapping("/wallets")
    @Operation(summary = "List all student wallets with balances")
    public ResponseEntity<Page<Wallet>> getWallets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 100);
        return ResponseEntity.ok(walletRepository.findAll(
                PageRequest.of(page, boundedLimit, Sort.by("balanceMinorUnits").descending())
        ));
    }
}
