package com.rit.token.ritz.merchant;

import com.rit.token.ritz.transaction.WalletTransaction;
import com.rit.token.ritz.transaction.WalletTransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/merchants")
@Tag(name = "Merchant Management", description = "Merchant management, sales monitoring and settlements")
public class MerchantController {

    private final MerchantService merchantService;
    private final MerchantRepository merchantRepository;
    private final MerchantAccountRepository accountRepository;
    private final MerchantSettlementRepository settlementRepository;
    private final WalletTransactionRepository transactionRepository;

    public MerchantController(
            MerchantService merchantService,
            MerchantRepository merchantRepository,
            MerchantAccountRepository accountRepository,
            MerchantSettlementRepository settlementRepository,
            WalletTransactionRepository transactionRepository) {
        this.merchantService = merchantService;
        this.merchantRepository = merchantRepository;
        this.accountRepository = accountRepository;
        this.settlementRepository = settlementRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    @Operation(summary = "List all registered campus merchants")
    public ResponseEntity<List<Merchant>> getAllMerchants() {
        return ResponseEntity.ok(merchantService.getAllMerchants());
    }

    @PostMapping
    @Operation(summary = "Register a new campus merchant (Admin only)")
    public ResponseEntity<Merchant> registerMerchant(@RequestBody Map<String, String> request) {
        String merchantId = request.get("merchant_id");
        String name = request.get("name");
        String description = request.get("description");
        String categoryStr = request.get("category");
        MerchantCategory category = categoryStr != null ? MerchantCategory.valueOf(categoryStr.toUpperCase()) : MerchantCategory.FOOD;
        String associatedAppId = request.get("associated_application_id");
        String email = request.get("contact_email");
        String phone = request.get("contact_phone");

        Merchant merchant = merchantService.registerMerchant(merchantId, name, description, category, associatedAppId, email, phone);
        return ResponseEntity.ok(merchant);
    }

    @GetMapping("/{merchantId}")
    @Operation(summary = "Get merchant details and financial metrics")
    public ResponseEntity<Map<String, Object>> getMerchantDetails(@PathVariable String merchantId) {
        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));
        MerchantAccount account = accountRepository.findByMerchantId(merchantId).orElse(null);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Long todaySales = transactionRepository.getMerchantVolumeSince(merchantId, startOfDay);
        Long totalSales = transactionRepository.getMerchantTotalVolume(merchantId);

        Map<String, Object> response = new HashMap<>();
        response.put("merchant", merchant);
        response.put("account", account);
        response.put("today_sales", todaySales);
        response.put("total_sales", totalSales);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{merchantId}/transactions")
    @Operation(summary = "Get transactions for a specific merchant")
    public ResponseEntity<Map<String, Object>> getMerchantTransactions(
            @PathVariable String merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<WalletTransaction> txns = transactionRepository.findByMerchantIdOrderByCreatedAtDesc(
                merchantId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("transactions", txns.getContent());
        response.put("total_elements", txns.getTotalElements());
        response.put("total_pages", txns.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{merchantId}/settlements")
    @Operation(summary = "Get settlements for a merchant")
    public ResponseEntity<List<MerchantSettlement>> getMerchantSettlements(@PathVariable String merchantId) {
        return ResponseEntity.ok(settlementRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId));
    }

    @PostMapping("/{merchantId}/settlements")
    @Operation(summary = "Request a settlement for accumulated merchant earnings")
    public ResponseEntity<MerchantSettlement> requestSettlement(
            @PathVariable String merchantId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long amount = Long.parseLong(request.get("amount").toString());
        String notes = (String) request.get("notes");
        String actorId = authentication != null ? authentication.getName() : "ADMIN";

        MerchantSettlement settlement = merchantService.requestSettlement(merchantId, amount, notes, actorId);
        return ResponseEntity.ok(settlement);
    }

    @PostMapping("/settlements/{settlementId}/complete")
    @Operation(summary = "Mark a bank settlement as completed (Finance Admin only)")
    public ResponseEntity<MerchantSettlement> completeSettlement(
            @PathVariable String settlementId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        String bankReference = request.get("bank_reference");
        String actorId = authentication != null ? authentication.getName() : "FINANCE_ADMIN";

        MerchantSettlement completed = merchantService.processSettlement(settlementId, bankReference, actorId);
        return ResponseEntity.ok(completed);
    }
}
