package com.rit.token.ritz.payment;

import com.rit.token.ritz.audit.AuditService;
import com.rit.token.ritz.config.system.SystemConfigService;
import com.rit.token.ritz.ledger.AccountType;
import com.rit.token.ritz.ledger.LedgerService;
import com.rit.token.ritz.notification.NotificationService;
import com.rit.token.ritz.notification.NotificationType;
import com.rit.token.ritz.transaction.TransactionStatus;
import com.rit.token.ritz.transaction.TransactionType;
import com.rit.token.ritz.transaction.WalletTransaction;
import com.rit.token.ritz.transaction.WalletTransactionRepository;
import com.rit.token.ritz.wallet.Wallet;
import com.rit.token.ritz.wallet.WalletRepository;
import com.rit.token.ritz.wallet.WalletStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class RechargeService {

    private static final Logger log = LoggerFactory.getLogger(RechargeService.class);

    private final RechargeOrderRepository rechargeOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final PaymentGatewayFactory gatewayFactory;
    private final SystemConfigService configService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public RechargeService(
            RechargeOrderRepository rechargeOrderRepository,
            PaymentRecordRepository paymentRecordRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository transactionRepository,
            LedgerService ledgerService,
            PaymentGatewayFactory gatewayFactory,
            SystemConfigService configService,
            NotificationService notificationService,
            AuditService auditService) {
        this.rechargeOrderRepository = rechargeOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
        this.gatewayFactory = gatewayFactory;
        this.configService = configService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    /**
     * Step 1 - 4: Create internal order and initialize payment gateway order
     */
    @Transactional
    public RechargeOrder createRechargeOrder(String studentId, BigDecimal amountRupees) {
        if (amountRupees == null) {
            throw new IllegalArgumentException("Recharge amount is required");
        }

        long min = configService.getMinRechargeInr();
        long max = configService.getMaxRechargeInr();
        if (amountRupees.compareTo(BigDecimal.valueOf(min)) < 0 || amountRupees.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw new IllegalArgumentException("Recharge amount must be between ₹" + min + " and ₹" + max);
        }

        Wallet wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for student: " + studentId));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active (Status: " + wallet.getStatus() + ")");
        }

        // Token conversion: amount in tokens = amountRupees / exchangeRate
        BigDecimal exchangeRate = configService.getTokenExchangeRate();
        long tokenAmount = amountRupees.divide(exchangeRate, 0, RoundingMode.FLOOR).longValue();

        String internalOrderId = "RCHG-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        PaymentGatewayService provider = gatewayFactory.getProvider();

        PaymentOrderResult gatewayResult = provider.createOrder(internalOrderId, amountRupees, studentId, internalOrderId);
        if (!gatewayResult.isSuccess()) {
            throw new IllegalStateException("Failed to initialize payment: " + gatewayResult.getErrorMessage());
        }

        RechargeOrder order = new RechargeOrder(internalOrderId, studentId, amountRupees, tokenAmount, provider.getProviderName());
        order.setGatewayOrderId(gatewayResult.getGatewayOrderId());
        return rechargeOrderRepository.save(order);
    }

    /**
     * Verifies payment and credits the student wallet atomically with double-entry accounting.
     * Guaranteed EXACTLY-ONCE execution.
     */
    @Transactional(noRollbackFor = SecurityException.class)
    public WalletTransaction processPaymentConfirmation(String orderId, String paymentId, String signature, String paymentMethod) {
        RechargeOrder order = rechargeOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Recharge order not found: " + orderId));

        // Idempotency: if already paid, return existing transaction
        if (order.getStatus() == PaymentOrderStatus.PAID) {
            log.info("Recharge order {} already paid. Returning existing transaction.", orderId);
            return transactionRepository.findByReferenceId(orderId)
                    .orElseThrow(() -> new IllegalStateException("Inconsistent state: paid order missing transaction"));
        }

        PaymentGatewayService provider = gatewayFactory.getProvider(order.getPaymentGateway());
        boolean signatureValid = provider.verifyPaymentSignature(order.getGatewayOrderId(), paymentId, signature);
        if (!signatureValid) {
            order.setStatus(PaymentOrderStatus.FAILED);
            rechargeOrderRepository.save(order);
            auditService.logAction(order.getStudentId(), "STUDENT", "RECHARGE_SIGNATURE_FAILED", "RECHARGE_ORDER", orderId, null, "Invalid payment signature");
            throw new SecurityException("Payment signature verification failed");
        }

        // Lock student wallet with pessimistic write lock
        Wallet wallet = walletRepository.findWithLockByStudentId(order.getStudentId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for student: " + order.getStudentId()));

        long previousBalance = wallet.getBalanceMinorUnits();
        long tokensToCredit = order.getTokenAmount();

        // 1. Credit wallet
        wallet.credit(tokensToCredit);
        walletRepository.save(wallet);

        // 2. Mark recharge order as PAID
        order.setStatus(PaymentOrderStatus.PAID);
        order.setPaymentId(paymentId);
        order.setSignature(signature);
        rechargeOrderRepository.save(order);

        // 3. Save payment record
        PaymentRecord record = new PaymentRecord(
                paymentId,
                orderId,
                order.getAmountRupees().multiply(new BigDecimal("100")).longValue(),
                order.getPaymentGateway(),
                "captured",
                paymentMethod != null ? paymentMethod : "UPI"
        );
        paymentRecordRepository.save(record);

        // 4. Record double-entry ledger entries
        // DEBIT: ASSET-RAZORPAY-CLEARING (Asset increases)
        // CREDIT: LIAB-WLT-{studentId} (Liability to student increases)
        String clearingAccount = "ASSET-" + order.getPaymentGateway() + "-CLEARING";
        String walletAccount = "LIAB-WLT-" + order.getStudentId();
        ledgerService.getOrCreateAccount(clearingAccount, "Gateway Clearing (" + order.getPaymentGateway() + ")", AccountType.ASSET);
        ledgerService.getOrCreateAccount(walletAccount, "Student Wallet (" + order.getStudentId() + ")", AccountType.LIABILITY);

        String txnId = "RZTXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        ledgerService.recordDoubleEntry(
                txnId,
                clearingAccount,
                walletAccount,
                tokensToCredit,
                "Wallet recharge via " + order.getPaymentGateway() + " (Order: " + orderId + ")"
        );

        // 5. Record WalletTransaction
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionId(txnId);
        transaction.setReferenceId(orderId);
        transaction.setSourceAccount(clearingAccount);
        transaction.setDestinationAccount(walletAccount);
        transaction.setAmountMinorUnits(tokensToCredit);
        transaction.setCurrency("RITZ");
        transaction.setTransactionType(TransactionType.RECHARGE);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setStudentId(order.getStudentId());
        transaction.setPreviousBalanceMinorUnits(previousBalance);
        transaction.setCurrentBalanceMinorUnits(wallet.getBalanceMinorUnits());
        transaction.setDescription("Wallet recharge ₹" + order.getAmountRupees() + " (" + tokensToCredit + " tokens)");
        WalletTransaction savedTxn = transactionRepository.save(transaction);

        // 6. In-app notification
        notificationService.sendNotification(
                order.getStudentId(),
                "STUDENT",
                "Recharge Successful",
                "Your wallet has been credited with " + tokensToCredit + " RITZ tokens (₹" + order.getAmountRupees() + ").",
                NotificationType.RECHARGE_SUCCESS
        );

        auditService.logAction(
                order.getStudentId(),
                "STUDENT",
                "WALLET_RECHARGE_COMPLETED",
                "WALLET",
                wallet.getWalletId(),
                null,
                "Credited " + tokensToCredit + " tokens via " + orderId
        );

        log.info("Recharge completed successfully: Order={} Student={} Amount={} Balance={}",
                orderId, order.getStudentId(), tokensToCredit, wallet.getBalanceMinorUnits());
        return savedTxn;
    }
}
