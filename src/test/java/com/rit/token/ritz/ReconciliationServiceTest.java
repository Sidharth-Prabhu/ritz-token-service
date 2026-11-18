package com.rit.token.ritz;

import com.rit.token.ritz.payment.RechargeOrder;
import com.rit.token.ritz.payment.RechargeService;
import com.rit.token.ritz.reconciliation.ReconciliationService;
import com.rit.token.ritz.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class ReconciliationServiceTest {

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private RechargeService rechargeService;

    @Autowired
    private WalletService walletService;

    private static final String RECON_STUDENT = "RITRECON001";

    @BeforeEach
    void setUp() {
        walletService.createStudentAndWallet(
                RECON_STUDENT, "Recon Student", "recon@ritchennai.edu.in", "9666655555", "MECH", 2026
        );
    }

    @Test
    @DisplayName("Financial reconciliation checks recharge records against payments and ledger entries")
    void testReconciliationExecution() {
        // Complete a legitimate recharge
        RechargeOrder order = rechargeService.createRechargeOrder(RECON_STUDENT, new BigDecimal("75.00"));
        rechargeService.processPaymentConfirmation(order.getOrderId(), "pay_recon_1", "mock_sig_valid", "UPI");

        ReconciliationService.ReconciliationReport report = reconciliationService.performReconciliation();

        assertNotNull(report);
        assertTrue(report.getTotalOrdersChecked() >= 1);
        assertTrue(report.getMatchedRecords() >= 1);
        assertTrue(report.isLedgerGloballyBalanced());
    }
}
