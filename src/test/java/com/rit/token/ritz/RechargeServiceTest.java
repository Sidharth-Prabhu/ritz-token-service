package com.rit.token.ritz;

import com.rit.token.ritz.payment.PaymentOrderStatus;
import com.rit.token.ritz.payment.RechargeOrder;
import com.rit.token.ritz.payment.RechargeOrderRepository;
import com.rit.token.ritz.payment.RechargeService;
import com.rit.token.ritz.transaction.WalletTransaction;
import com.rit.token.ritz.wallet.Wallet;
import com.rit.token.ritz.wallet.WalletRepository;
import com.rit.token.ritz.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class RechargeServiceTest {

    @Autowired
    private RechargeService rechargeService;

    @Autowired
    private RechargeOrderRepository rechargeOrderRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    private static final String RECHARGE_STUDENT = "RITRECHARGE01";

    @BeforeEach
    void setUp() {
        if (walletRepository.findByStudentId(RECHARGE_STUDENT).isEmpty()) {
            Wallet wallet = walletService.createStudentAndWallet(
                    RECHARGE_STUDENT, "Recharge Tester", "recharge@ritchennai.edu.in", "9777788888", "ECE", 2026
            );
            wallet.setBalanceMinorUnits(0L);
            walletRepository.save(wallet);
        } else {
            Wallet wallet = walletRepository.findByStudentId(RECHARGE_STUDENT).get();
            wallet.setBalanceMinorUnits(0L);
            walletRepository.save(wallet);
        }
    }

    @Test
    @DisplayName("Create recharge order initializes internal order and gateway order")
    void testCreateRechargeOrder() {
        RechargeOrder order = rechargeService.createRechargeOrder(RECHARGE_STUDENT, new BigDecimal("200.00"));

        assertNotNull(order.getOrderId());
        assertEquals(RECHARGE_STUDENT, order.getStudentId());
        assertEquals(new BigDecimal("200.00"), order.getAmountRupees());
        assertEquals(200L, order.getTokenAmount());
        assertEquals(PaymentOrderStatus.CREATED, order.getStatus());
        assertNotNull(order.getGatewayOrderId());
    }

    @Test
    @DisplayName("Payment confirmation credits wallet exactly once and updates order to PAID")
    void testSuccessfulPaymentConfirmation() {
        RechargeOrder order = rechargeService.createRechargeOrder(RECHARGE_STUDENT, new BigDecimal("150.00"));

        // Process payment with valid sandbox signature
        WalletTransaction txn = rechargeService.processPaymentConfirmation(
                order.getOrderId(),
                "pay_test_" + System.currentTimeMillis(),
                "mock_sig_valid",
                "UPI"
        );

        assertNotNull(txn);
        assertEquals(150L, txn.getAmountMinorUnits());
        assertEquals(150L, txn.getCurrentBalanceMinorUnits());

        // Verify wallet balance
        Wallet wallet = walletRepository.findByStudentId(RECHARGE_STUDENT).orElseThrow();
        assertEquals(150L, wallet.getBalanceMinorUnits());

        // Idempotency: Process same payment again
        WalletTransaction secondTxn = rechargeService.processPaymentConfirmation(
                order.getOrderId(),
                "pay_duplicate",
                "mock_sig_valid",
                "UPI"
        );

        assertEquals(txn.getTransactionId(), secondTxn.getTransactionId());
        // Balance must still be 150L, not 300L
        wallet = walletRepository.findByStudentId(RECHARGE_STUDENT).orElseThrow();
        assertEquals(150L, wallet.getBalanceMinorUnits());
    }

    @Test
    @DisplayName("Invalid payment signature throws SecurityException and marks order FAILED")
    void testInvalidPaymentSignature() {
        RechargeOrder order = rechargeService.createRechargeOrder(RECHARGE_STUDENT, new BigDecimal("100.00"));

        assertThrows(SecurityException.class, () -> {
            rechargeService.processPaymentConfirmation(
                    order.getOrderId(),
                    "pay_fraud",
                    "invalid_signature_tampered",
                    "UPI"
            );
        });

        RechargeOrder updatedOrder = rechargeOrderRepository.findByOrderId(order.getOrderId()).orElseThrow();
        assertEquals(PaymentOrderStatus.FAILED, updatedOrder.getStatus());

        Wallet wallet = walletRepository.findByStudentId(RECHARGE_STUDENT).orElseThrow();
        assertEquals(0L, wallet.getBalanceMinorUnits());
    }
}
