package com.rit.token.ritz.controller;

import com.rit.token.ritz.dto.*;
import com.rit.token.ritz.entity.TokenTransaction;
import com.rit.token.ritz.entity.TokenUnit;
import com.rit.token.ritz.entity.TokenWallet;
import com.rit.token.ritz.service.RitzTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tokens")
@Tag(name = "Ritz Token Controller", description = "Endpoints for managing Ritz digital tokens, wallets, circulation, and transactions")
public class RitzTokenController {

    @Autowired
    private RitzTokenService tokenService;

    @Operation(summary = "Get user wallet balance")
    @GetMapping("/balance/{userId}")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable Long userId) {
        BigDecimal balance = tokenService.getBalance(userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "balance", balance,
                "currency", "Ritz Token"
        ));
    }

    @Operation(summary = "Get detailed wallet information")
    @GetMapping("/wallet/{userId}")
    public ResponseEntity<?> getWallet(@PathVariable Long userId) {
        return tokenService.getWallet(userId)
                .map(w -> ResponseEntity.ok(new WalletResponse(
                        w.getUserId(),
                        w.getUserName(),
                        w.getMobileNumber(),
                        w.getBalance(),
                        w.getUpdatedAt()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get all wallets with their current balances")
    @GetMapping("/wallets")
    public ResponseEntity<List<WalletResponse>> getAllWallets() {
        List<WalletResponse> wallets = tokenService.getAllWallets().stream()
                .map(w -> new WalletResponse(w.getUserId(), w.getUserName(), w.getMobileNumber(), w.getBalance(), w.getUpdatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(wallets);
    }

    @Operation(summary = "Credit / Mint Ritz tokens into a user's wallet")
    @PostMapping("/topup")
    public ResponseEntity<?> topUp(@Valid @RequestBody TopUpRequest request) {
        try {
            TokenWallet updatedWallet = tokenService.topUp(
                    request.getUserId(),
                    request.getUserName(),
                    request.getMobileNumber(),
                    request.getAmount(),
                    request.getReferenceId() != null ? request.getReferenceId() : "TOPUP-" + System.currentTimeMillis(),
                    request.getDescription()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", updatedWallet.getUserId(),
                    "newBalance", updatedWallet.getBalance(),
                    "amountCredited", request.getAmount(),
                    "message", "Successfully added " + request.getAmount() + " Ritz Tokens"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @Operation(summary = "Spend / Burn Ritz tokens for food or order payment")
    @PostMapping("/spend")
    public ResponseEntity<?> spend(@Valid @RequestBody SpendRequest request) {
        try {
            TokenWallet updatedWallet = tokenService.spend(
                    request.getUserId(),
                    request.getAmount(),
                    request.getOrderRef(),
                    request.getDescription()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", updatedWallet.getUserId(),
                    "newBalance", updatedWallet.getBalance(),
                    "amountDeducted", request.getAmount(),
                    "orderRef", request.getOrderRef(),
                    "message", "Successfully spent " + request.getAmount() + " Ritz Tokens"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @Operation(summary = "Refund / Remint Ritz tokens back to a user's wallet")
    @PostMapping("/refund")
    public ResponseEntity<?> refund(@Valid @RequestBody RefundRequest request) {
        try {
            TokenWallet updatedWallet = tokenService.refund(
                    request.getUserId(),
                    request.getAmount(),
                    request.getOrderRef(),
                    request.getReason()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", updatedWallet.getUserId(),
                    "newBalance", updatedWallet.getBalance(),
                    "amountRefunded", request.getAmount(),
                    "orderRef", request.getOrderRef(),
                    "message", "Successfully refunded " + request.getAmount() + " Ritz Tokens"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @Operation(summary = "Get user token transactions")
    @GetMapping("/transactions/{userId}")
    public ResponseEntity<List<TokenTransaction>> getUserTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(tokenService.getTransactions(userId));
    }

    @Operation(summary = "Get all system token transactions")
    @GetMapping("/transactions")
    public ResponseEntity<List<TokenTransaction>> getAllTransactions() {
        return ResponseEntity.ok(tokenService.getAllTransactions());
    }

    @Operation(summary = "Get system-wide Ritz stats (total circulation, active wallets, serialized total)")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(tokenService.getGlobalStats().toMap());
    }

    @Operation(summary = "Get paginated list of all serialized token units in circulation")
    @GetMapping("/circulation")
    public ResponseEntity<Page<TokenUnit>> getCirculation(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(tokenService.getCirculation(PageRequest.of(page, size)));
    }
}
