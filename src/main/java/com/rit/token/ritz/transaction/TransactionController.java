package com.rit.token.ritz.transaction;

import com.rit.token.ritz.application.ApplicationService;
import com.rit.token.ritz.application.PermissionScope;
import com.rit.token.ritz.transaction.dto.CreditRequest;
import com.rit.token.ritz.transaction.dto.DebitRequest;
import com.rit.token.ritz.transaction.dto.RefundApiRequest;
import com.rit.token.ritz.transaction.dto.TransactionApiResponse;
import com.rit.token.ritz.wallet.Student;
import com.rit.token.ritz.wallet.StudentRepository;
import com.rit.token.ritz.wallet.Wallet;
import com.rit.token.ritz.wallet.WalletRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Application Integration API", description = "Standardized REST API for external campus applications (Tillo, ERP, Stationery, Transport)")
public class TransactionController {

    private final TransactionService transactionService;
    private final WalletTransactionRepository transactionRepository;
    private final ApplicationService applicationService;
    private final WalletRepository walletRepository;
    private final StudentRepository studentRepository;

    public TransactionController(
            TransactionService transactionService,
            WalletTransactionRepository transactionRepository,
            ApplicationService applicationService,
            WalletRepository walletRepository,
            StudentRepository studentRepository) {
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
        this.applicationService = applicationService;
        this.walletRepository = walletRepository;
        this.studentRepository = studentRepository;
    }

    private String getAppId(HttpServletRequest request, Authentication authentication) {
        Object attr = request.getAttribute("authenticatedApplicationId");
        if (attr != null) return attr.toString();
        return authentication != null ? authentication.getName() : "EXTERNAL_APP";
    }

    @PostMapping("/debit")
    @Operation(summary = "Deduct tokens from a student wallet (supports student_id or phone_number)")
    public ResponseEntity<TransactionApiResponse> debit(
            @Valid @RequestBody DebitRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String appId = getAppId(httpRequest, authentication);

        // Scope check for registered application (skip check for admin role)
        if (authentication != null && authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().contains("ADMIN"))) {
            if (!"MASTER_INTEGRATION".equals(appId) && !applicationService.hasPermission(appId, PermissionScope.TRANSACTION_DEBIT)) {
                return ResponseEntity.status(403).body(TransactionApiResponse.failure("Application lacks TRANSACTION_DEBIT permission", request.getReferenceId()));
            }
        }

        TransactionApiResponse response = transactionService.executeDebit(request, idempotencyKey, appId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund a previously completed transaction")
    public ResponseEntity<TransactionApiResponse> refund(
            @Valid @RequestBody RefundApiRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String appId = getAppId(httpRequest, authentication);

        if (authentication != null && authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().contains("ADMIN"))) {
            if (!"MASTER_INTEGRATION".equals(appId) && !applicationService.hasPermission(appId, PermissionScope.TRANSACTION_REFUND)) {
                return ResponseEntity.status(403).body(TransactionApiResponse.failure("Application lacks TRANSACTION_REFUND permission", request.getReferenceId()));
            }
        }

        TransactionApiResponse response = transactionService.executeRefund(request, idempotencyKey, appId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate if student wallet has sufficient funds (supports student_id or phone_number)")
    public ResponseEntity<Map<String, Object>> validateBalance(
            @RequestBody Map<String, Object> request) {

        String studentId = (String) request.get("student_id");
        String phoneNumber = (String) request.get("phone_number");
        Object amountObj = request.get("amount");

        if (amountObj == null) {
            throw new IllegalArgumentException("amount is required");
        }
        long requiredAmount = Long.parseLong(amountObj.toString());

        String resolvedStudentId = studentId;
        if ((resolvedStudentId == null || resolvedStudentId.isBlank()) && (phoneNumber != null && !phoneNumber.isBlank())) {
            String rawPhone = phoneNumber;
            String cleanedDigits = rawPhone.replaceAll("[^0-9]", "");
            if (cleanedDigits.length() > 10) {
                cleanedDigits = cleanedDigits.substring(cleanedDigits.length() - 10);
            }
            final String phoneToFind = cleanedDigits;

            Optional<Student> studentOpt = studentRepository.findByMobileNumber(phoneToFind)
                    .or(() -> studentRepository.findByMobileNumber(rawPhone));

            if (studentOpt.isEmpty()) {
                throw new IllegalArgumentException("No student wallet found with phone number: " + rawPhone);
            }
            resolvedStudentId = studentOpt.get().getStudentId();
        }

        if (resolvedStudentId == null || resolvedStudentId.isBlank()) {
            throw new IllegalArgumentException("Either student_id or phone_number must be provided");
        }

        final String effectiveStudentId = resolvedStudentId;

        Wallet wallet = walletRepository.findByStudentId(effectiveStudentId)
                .orElseThrow(() -> new IllegalArgumentException("Student wallet not found: " + effectiveStudentId));

        boolean sufficient = wallet.canDebit(requiredAmount);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", sufficient);
        response.put("student_id", effectiveStudentId);
        response.put("available_balance", wallet.getBalanceMinorUnits());
        response.put("required_amount", requiredAmount);
        response.put("wallet_status", wallet.getStatus());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Retrieve transaction details by ID")
    public ResponseEntity<WalletTransaction> getTransaction(@PathVariable String transactionId) {
        WalletTransaction txn = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        return ResponseEntity.ok(txn);
    }
}
