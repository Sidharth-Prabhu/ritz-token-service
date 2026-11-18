package com.rit.token.ritz.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rit.token.ritz.audit.AuditService;
import com.rit.token.ritz.config.system.SystemConfigService;
import com.rit.token.ritz.ledger.AccountType;
import com.rit.token.ritz.ledger.LedgerService;
import com.rit.token.ritz.merchant.Merchant;
import com.rit.token.ritz.merchant.MerchantAccount;
import com.rit.token.ritz.merchant.MerchantAccountRepository;
import com.rit.token.ritz.merchant.MerchantRepository;
import com.rit.token.ritz.notification.NotificationService;
import com.rit.token.ritz.notification.NotificationType;
import com.rit.token.ritz.transaction.dto.DebitRequest;
import com.rit.token.ritz.transaction.dto.RefundApiRequest;
import com.rit.token.ritz.transaction.dto.TransactionApiResponse;
import com.rit.token.ritz.wallet.Student;
import com.rit.token.ritz.wallet.StudentRepository;
import com.rit.token.ritz.wallet.Wallet;
import com.rit.token.ritz.wallet.WalletRepository;
import com.rit.token.ritz.wallet.WalletStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final WalletRepository walletRepository;
    private final StudentRepository studentRepository;
    private final WalletTransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final MerchantRepository merchantRepository;
    private final MerchantAccountRepository merchantAccountRepository;
    private final IdempotencyService idempotencyService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SystemConfigService configService;
    private final ObjectMapper objectMapper;

    public TransactionService(
            WalletRepository walletRepository,
            StudentRepository studentRepository,
            WalletTransactionRepository transactionRepository,
            LedgerService ledgerService,
            MerchantRepository merchantRepository,
            MerchantAccountRepository merchantAccountRepository,
            IdempotencyService idempotencyService,
            NotificationService notificationService,
            AuditService auditService,
            SystemConfigService configService,
            ObjectMapper objectMapper) {
        this.walletRepository = walletRepository;
        this.studentRepository = studentRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
        this.merchantRepository = merchantRepository;
        this.merchantAccountRepository = merchantAccountRepository;
        this.idempotencyService = idempotencyService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    /**
     * Resolves student ID from either explicit studentId or phoneNumber.
     */
    private String resolveStudentId(DebitRequest request) {
        if (request.getStudentId() != null && !request.getStudentId().isBlank()) {
            return request.getStudentId();
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            String rawPhone = request.getPhoneNumber();
            String cleanedDigits = rawPhone.replaceAll("[^0-9]", "");
            if (cleanedDigits.length() > 10) {
                cleanedDigits = cleanedDigits.substring(cleanedDigits.length() - 10);
            }
            final String phoneToFind = cleanedDigits;

            return studentRepository.findByMobileNumber(phoneToFind)
                    .or(() -> studentRepository.findByMobileNumber(rawPhone))
                    .map(Student::getStudentId)
                    .orElseThrow(() -> new IllegalArgumentException("No student wallet found with phone number: " + rawPhone));
        }

        throw new IllegalArgumentException("Either student_id or phone_number must be provided in debit request");
    }

    /**
     * Executes a student debit (canteen purchase, stationery, etc.)
     * Concurrency: Uses pessimistic row-level locking on the wallet.
     * Idempotency: Duplicate submissions with the same idempotency key return the cached original response.
     */
    @Transactional
    public TransactionApiResponse executeDebit(DebitRequest request, String idempotencyKey, String applicationId) {
        String scope = "DEBIT";

        // 1. Check idempotency
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyRecord> existing = idempotencyService.findRecord(idempotencyKey, scope);
            if (existing.isPresent()) {
                log.info("Idempotent debit request detected for key: {}. Returning cached response.", idempotencyKey);
                try {
                    return objectMapper.readValue(existing.get().getResponseBody(), TransactionApiResponse.class);
                } catch (Exception e) {
                    log.error("Failed to parse cached response: {}", e.getMessage());
                }
            }
        }

        // 2. Resolve student ID (supports both student_id and phone_number)
        String effectiveStudentId = resolveStudentId(request);

        // 3. Resolve merchant (default to TILLO or MASTER if not specified)
        String merchantId = request.getMerchantId();
        if (merchantId == null || merchantId.isBlank()) {
            merchantId = "TILLO";
        }
        final String finalMerchantId = merchantId;

        Merchant merchant = merchantRepository.findByMerchantId(finalMerchantId)
                .orElseGet(() -> {
                    // Fallback to first available active merchant or create placeholder
                    return merchantRepository.findAll().stream()
                            .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + finalMerchantId));
                });

        if (!"ACTIVE".equalsIgnoreCase(merchant.getStatus())) {
            throw new IllegalStateException("Merchant is not active");
        }

        // 4. Acquire pessimistic write lock on student wallet
        Wallet wallet = walletRepository.findWithLockByStudentId(effectiveStudentId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for student: " + effectiveStudentId));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active (Status: " + wallet.getStatus() + ")");
        }

        long previousBalance = wallet.getBalanceMinorUnits();
        long amount = request.getAmount();

        if (previousBalance < amount) {
            log.warn("Debit rejected: Student {} insufficient funds. Required: {}, Available: {}",
                    effectiveStudentId, amount, previousBalance);
            throw new IllegalArgumentException("Insufficient wallet balance. Available: " + previousBalance + " RITZ tokens");
        }

        // 5. Deduct from wallet
        wallet.debit(amount);
        walletRepository.save(wallet);

        // 6. Update merchant account
        MerchantAccount merchantAccount = merchantAccountRepository.findWithLockByMerchantId(merchant.getMerchantId())
                .orElseGet(() -> {
                    MerchantAccount ma = new MerchantAccount(merchant.getMerchantId());
                    return merchantAccountRepository.save(ma);
                });
        merchantAccount.creditEarnings(amount);
        merchantAccountRepository.save(merchantAccount);

        // 7. Double-entry ledger recording
        // DEBIT: LIAB-WLT-{studentId} (Liability decreases)
        // CREDIT: LIAB-MERCH-{merchantId} (Merchant liability increases)
        String studentAccount = "LIAB-WLT-" + effectiveStudentId;
        String merchantAccNum = "LIAB-MERCH-" + merchant.getMerchantId();
        ledgerService.getOrCreateAccount(studentAccount, "Student Wallet (" + effectiveStudentId + ")", AccountType.LIABILITY);
        ledgerService.getOrCreateAccount(merchantAccNum, "Merchant Payable (" + merchant.getMerchantId() + ")", AccountType.LIABILITY);

        String txnId = "RZTXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        ledgerService.recordDoubleEntry(
                txnId,
                studentAccount,
                merchantAccNum,
                amount,
                request.getDescription() != null ? request.getDescription() : "Purchase at " + merchant.getName()
        );

        // 8. Save Transaction entity
        WalletTransaction txn = new WalletTransaction();
        txn.setTransactionId(txnId);
        txn.setReferenceId(request.getReferenceId());
        txn.setIdempotencyKey(idempotencyKey);
        txn.setSourceAccount(studentAccount);
        txn.setDestinationAccount(merchantAccNum);
        txn.setAmountMinorUnits(amount);
        txn.setCurrency("RITZ");
        txn.setTransactionType(TransactionType.DEBIT);
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setInitiatingApplicationId(applicationId);
        txn.setMerchantId(merchant.getMerchantId());
        txn.setStudentId(effectiveStudentId);
        txn.setDescription(request.getDescription() != null ? request.getDescription() : "Campus purchase");
        txn.setPreviousBalanceMinorUnits(previousBalance);
        txn.setCurrentBalanceMinorUnits(wallet.getBalanceMinorUnits());
        transactionRepository.save(txn);

        // 9. Notifications
        notificationService.sendNotification(
                effectiveStudentId,
                "STUDENT",
                "Purchase Successful",
                "Paid " + amount + " tokens at " + merchant.getName() + " (Ref: " + request.getReferenceId() + "). Balance: " + wallet.getBalanceMinorUnits(),
                NotificationType.PURCHASE
        );

        long lowBalanceThreshold = configService.getLowBalanceThreshold();
        if (wallet.getBalanceMinorUnits() <= lowBalanceThreshold) {
            notificationService.sendNotification(
                    effectiveStudentId,
                    "STUDENT",
                    "Low Wallet Balance Alert",
                    "Your wallet balance is low: " + wallet.getBalanceMinorUnits() + " tokens remaining. Please recharge soon.",
                    NotificationType.LOW_BALANCE
            );
        }

        TransactionApiResponse response = TransactionApiResponse.success(
                txnId,
                request.getReferenceId(),
                amount,
                previousBalance,
                wallet.getBalanceMinorUnits(),
                TransactionStatus.COMPLETED
        );

        // 10. Save Idempotency record
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                String responseJson = objectMapper.writeValueAsString(response);
                idempotencyService.saveRecord(idempotencyKey, scope, idempotencyService.computeHash(responseJson), 200, responseJson);
            } catch (Exception e) {
                log.warn("Failed to cache idempotency response: {}", e.getMessage());
            }
        }

        auditService.logAction(
                applicationId != null ? applicationId : effectiveStudentId,
                "APPLICATION",
                "TRANSACTION_DEBIT",
                "WALLET",
                wallet.getWalletId(),
                null,
                "Debited " + amount + " tokens for " + request.getReferenceId()
        );

        return response;
    }

    /**
     * Executes a transaction refund (compensating transaction)
     */
    @Transactional
    public TransactionApiResponse executeRefund(RefundApiRequest request, String idempotencyKey, String applicationId) {
        String scope = "REFUND";
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyRecord> existing = idempotencyService.findRecord(idempotencyKey, scope);
            if (existing.isPresent()) {
                try {
                    return objectMapper.readValue(existing.get().getResponseBody(), TransactionApiResponse.class);
                } catch (Exception ignored) {}
            }
        }

        WalletTransaction originalTxn = transactionRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Original transaction not found: " + request.getTransactionId()));

        if (originalTxn.getTransactionType() != TransactionType.DEBIT) {
            throw new IllegalArgumentException("Can only refund DEBIT transactions");
        }

        Wallet wallet = walletRepository.findWithLockByStudentId(originalTxn.getStudentId())
                .orElseThrow(() -> new IllegalStateException("Student wallet not found"));

        long previousBalance = wallet.getBalanceMinorUnits();
        long refundAmount = request.getAmount();

        // 1. Credit wallet back
        wallet.credit(refundAmount);
        walletRepository.save(wallet);

        // 2. Adjust merchant account
        if (originalTxn.getMerchantId() != null) {
            merchantAccountRepository.findWithLockByMerchantId(originalTxn.getMerchantId())
                    .ifPresent(ma -> {
                        ma.debitForRefund(refundAmount);
                        merchantAccountRepository.save(ma);
                    });
        }

        // 3. Compensating double-entry:
        // DEBIT: LIAB-MERCH-{merchantId} (Merchant liability decreases)
        // CREDIT: LIAB-WLT-{studentId} (Student wallet liability increases back)
        String merchantAccNum = "LIAB-MERCH-" + originalTxn.getMerchantId();
        String studentAccount = "LIAB-WLT-" + originalTxn.getStudentId();
        String refundTxnId = "RZTXN-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ledgerService.recordDoubleEntry(
                refundTxnId,
                merchantAccNum,
                studentAccount,
                refundAmount,
                "Refund for " + originalTxn.getTransactionId() + " (" + request.getReason() + ")"
        );

        // 4. Save refund transaction
        WalletTransaction refundTxn = new WalletTransaction();
        refundTxn.setTransactionId(refundTxnId);
        refundTxn.setReferenceId(request.getReferenceId() != null ? request.getReferenceId() : originalTxn.getReferenceId());
        refundTxn.setIdempotencyKey(idempotencyKey);
        refundTxn.setSourceAccount(merchantAccNum);
        refundTxn.setDestinationAccount(studentAccount);
        refundTxn.setAmountMinorUnits(refundAmount);
        refundTxn.setCurrency("RITZ");
        refundTxn.setTransactionType(TransactionType.REFUND);
        refundTxn.setStatus(TransactionStatus.COMPLETED);
        refundTxn.setInitiatingApplicationId(applicationId);
        refundTxn.setMerchantId(originalTxn.getMerchantId());
        refundTxn.setStudentId(originalTxn.getStudentId());
        refundTxn.setDescription("Refund: " + request.getReason());
        refundTxn.setPreviousBalanceMinorUnits(previousBalance);
        refundTxn.setCurrentBalanceMinorUnits(wallet.getBalanceMinorUnits());
        transactionRepository.save(refundTxn);

        // 5. Notification
        notificationService.sendNotification(
                originalTxn.getStudentId(),
                "STUDENT",
                "Refund Processed",
                "Refund of " + refundAmount + " tokens credited to your wallet (Ref: " + refundTxnId + ").",
                NotificationType.REFUND
        );

        TransactionApiResponse response = TransactionApiResponse.success(
                refundTxnId,
                refundTxn.getReferenceId(),
                refundAmount,
                previousBalance,
                wallet.getBalanceMinorUnits(),
                TransactionStatus.COMPLETED
        );

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                String responseJson = objectMapper.writeValueAsString(response);
                idempotencyService.saveRecord(idempotencyKey, scope, idempotencyService.computeHash(responseJson), 200, responseJson);
            } catch (Exception ignored) {}
        }

        return response;
    }
}
