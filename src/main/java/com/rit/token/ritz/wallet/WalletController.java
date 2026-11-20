package com.rit.token.ritz.wallet;

import com.rit.token.ritz.auth.User;
import com.rit.token.ritz.payment.RechargeOrder;
import com.rit.token.ritz.payment.RechargeOrderRepository;
import com.rit.token.ritz.payment.RechargeService;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
@Tag(name = "Student Wallet API", description = "Student wallet management and balances")
public class WalletController {

    private final WalletService walletService;
    private final RechargeService rechargeService;
    private final RechargeOrderRepository rechargeOrderRepository;
    private final WalletTransactionRepository transactionRepository;
    private final StudentRepository studentRepository;

    public WalletController(
            WalletService walletService,
            RechargeService rechargeService,
            RechargeOrderRepository rechargeOrderRepository,
            WalletTransactionRepository transactionRepository,
            StudentRepository studentRepository) {
        this.walletService = walletService;
        this.rechargeService = rechargeService;
        this.rechargeOrderRepository = rechargeOrderRepository;
        this.transactionRepository = transactionRepository;
        this.studentRepository = studentRepository;
    }

    private String resolveStudentId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof User user && user.getStudentId() != null) {
            return user.getStudentId();
        }
        return auth != null ? auth.getName() : null;
    }

    @GetMapping("/public")
    @Operation(summary = "Get wallet details publicly by student_id or phone_number (No auth required)")
    public ResponseEntity<Map<String, Object>> getWalletPublic(
            @RequestParam(required = false) String student_id,
            @RequestParam(required = false) String phone) {

        String effectiveId = student_id;
        if ((effectiveId == null || effectiveId.isBlank()) && (phone != null && !phone.isBlank())) {
            String cleanedDigits = phone.replaceAll("[^0-9]", "");
            if (cleanedDigits.length() > 10) {
                cleanedDigits = cleanedDigits.substring(cleanedDigits.length() - 10);
            }
            final String phoneToFind = cleanedDigits;

            effectiveId = studentRepository.findByMobileNumber(phoneToFind)
                    .or(() -> studentRepository.findByMobileNumber(phone))
                    .map(Student::getStudentId)
                    .orElse(null);

            if (effectiveId == null) {
                // Auto create student and wallet
                Wallet created = walletService.createStudentAndWallet(
                        "STU" + phoneToFind,
                        "Student " + phoneToFind,
                        phoneToFind + "@campus.rit.edu.in",
                        phoneToFind,
                        "General",
                        2026
                );
                effectiveId = created.getStudentId();
            }
        }

        if (effectiveId == null || effectiveId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "student_id or phone parameter required"));
        }

        final String finalId = effectiveId;
        Wallet wallet = walletService.getWallet(finalId)
                .orElseGet(() -> walletService.createStudentAndWallet(
                        finalId,
                        "Student " + finalId,
                        finalId.toLowerCase() + "@campus.rit.edu.in",
                        "9876543210",
                        "Engineering",
                        2026
                ));
        Student student = walletService.getStudent(finalId).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("wallet_id", wallet.getWalletId());
        response.put("student_id", wallet.getStudentId());
        response.put("student_name", student != null ? student.getName() : effectiveId);
        response.put("mobile_number", student != null ? student.getMobileNumber() : "");
        response.put("balance", wallet.getBalanceMinorUnits());
        response.put("currency", wallet.getCurrency());
        response.put("status", wallet.getStatus());
        response.put("last_transaction_at", wallet.getLastTransactionAt());
        response.put("created_at", wallet.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/public")
    @Operation(summary = "Get student wallet transaction history publicly without authentication (Razorpay verification mode)")
    public ResponseEntity<Map<String, Object>> getTransactionsPublic(
            @RequestParam(required = false) String student_id,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String effectiveId = student_id;
        if ((effectiveId == null || effectiveId.isBlank()) && (phone != null && !phone.isBlank())) {
            String cleanedDigits = phone.replaceAll("[^0-9]", "");
            if (cleanedDigits.length() > 10) {
                cleanedDigits = cleanedDigits.substring(cleanedDigits.length() - 10);
            }
            final String phoneToFind = cleanedDigits;
            effectiveId = studentRepository.findByMobileNumber(phoneToFind)
                    .or(() -> studentRepository.findByMobileNumber(phone))
                    .map(Student::getStudentId)
                    .orElse(null);
        }

        if (effectiveId == null || effectiveId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "student_id or phone parameter required"));
        }

        Page<WalletTransaction> txns = transactionRepository.findByStudentIdOrderByCreatedAtDesc(
                effectiveId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("transactions", txns.getContent());
        response.put("total_elements", txns.getTotalElements());
        response.put("total_pages", txns.getTotalPages());
        response.put("current_page", txns.getNumber());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/recharge/public")
    @Operation(summary = "Initialize a wallet recharge order publicly without authentication (Razorpay verification mode)")
    public ResponseEntity<Map<String, Object>> initiateRechargePublic(@RequestBody Map<String, Object> request) {
        String studentId = (String) request.get("student_id");
        String phone = (String) request.get("phone");
        Object amountObj = request.get("amount");

        if (amountObj == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        BigDecimal amountRupees = new BigDecimal(amountObj.toString());

        if ((studentId == null || studentId.isBlank()) && (phone != null && !phone.isBlank())) {
            String cleanedDigits = phone.replaceAll("[^0-9]", "");
            if (cleanedDigits.length() > 10) {
                cleanedDigits = cleanedDigits.substring(cleanedDigits.length() - 10);
            }
            final String phoneToFind = cleanedDigits;

            studentId = studentRepository.findByMobileNumber(phoneToFind)
                    .or(() -> studentRepository.findByMobileNumber(phone))
                    .map(Student::getStudentId)
                    .orElse(null);

            if (studentId == null) {
                Wallet created = walletService.createStudentAndWallet(
                        "STU" + phoneToFind,
                        "Student " + phoneToFind,
                        phoneToFind + "@campus.rit.edu.in",
                        phoneToFind,
                        "General",
                        2026
                );
                studentId = created.getStudentId();
            }
        }

        if (studentId == null || studentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "student_id or phone parameter required"));
        }

        RechargeOrder order = rechargeService.createRechargeOrder(studentId, amountRupees);
        Student student = walletService.getStudent(studentId).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("id", order.getId());
        response.put("orderId", order.getOrderId());
        response.put("studentId", order.getStudentId());
        response.put("amountRupees", order.getAmountRupees());
        response.put("tokenAmount", order.getTokenAmount());
        response.put("gatewayOrderId", order.getGatewayOrderId());
        response.put("paymentGateway", order.getPaymentGateway());
        response.put("status", order.getStatus());
        response.put("student_name", student != null ? student.getName() : studentId);
        response.put("mobile_number", student != null ? student.getMobileNumber() : "");

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get current student wallet balance and details")
    public ResponseEntity<Map<String, Object>> getWallet(Authentication authentication) {
        String studentId = resolveStudentId(authentication);
        if (studentId == null) return ResponseEntity.status(401).build();

        Wallet wallet = walletService.getWallet(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for student: " + studentId));
        Student student = walletService.getStudent(studentId).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("wallet_id", wallet.getWalletId());
        response.put("student_id", wallet.getStudentId());
        response.put("student_name", student != null ? student.getName() : studentId);
        response.put("balance", wallet.getBalanceMinorUnits());
        response.put("currency", wallet.getCurrency());
        response.put("status", wallet.getStatus());
        response.put("last_transaction_at", wallet.getLastTransactionAt());
        response.put("created_at", wallet.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get student wallet transaction history")
    public ResponseEntity<Map<String, Object>> getTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String studentId = resolveStudentId(authentication);
        if (studentId == null) return ResponseEntity.status(401).build();

        Page<WalletTransaction> txns = transactionRepository.findByStudentIdOrderByCreatedAtDesc(
                studentId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("transactions", txns.getContent());
        response.put("total_elements", txns.getTotalElements());
        response.put("total_pages", txns.getTotalPages());
        response.put("current_page", txns.getNumber());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/{transactionId}")
    @Operation(summary = "Get transaction details by transaction ID")
    public ResponseEntity<WalletTransaction> getTransaction(
            Authentication authentication,
            @PathVariable String transactionId) {
        String studentId = resolveStudentId(authentication);
        WalletTransaction txn = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Students can only view their own transactions unless admin
        if (studentId != null && !studentId.equals(txn.getStudentId()) &&
            authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("ROLE_FINANCE_ADMIN"))) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(txn);
    }

    @PostMapping("/recharge")
    @Operation(summary = "Initialize a wallet recharge order")
    public ResponseEntity<RechargeOrder> initiateRecharge(
            Authentication authentication,
            @RequestBody Map<String, Object> request) {
        String studentId = resolveStudentId(authentication);
        if (studentId == null) return ResponseEntity.status(401).build();

        Object amountObj = request.get("amount");
        if (amountObj == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        BigDecimal amountRupees = new BigDecimal(amountObj.toString());

        RechargeOrder order = rechargeService.createRechargeOrder(studentId, amountRupees);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/recharge/{rechargeId}")
    @Operation(summary = "Get recharge order status")
    public ResponseEntity<RechargeOrder> getRechargeStatus(
            Authentication authentication,
            @PathVariable String rechargeId) {
        RechargeOrder order = rechargeOrderRepository.findByOrderId(rechargeId)
                .orElseThrow(() -> new IllegalArgumentException("Recharge order not found: " + rechargeId));
        return ResponseEntity.ok(order);
    }
}
