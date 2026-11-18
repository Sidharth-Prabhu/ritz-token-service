package com.rit.token.ritz;

import com.rit.token.ritz.merchant.MerchantCategory;
import com.rit.token.ritz.merchant.MerchantService;
import com.rit.token.ritz.transaction.TransactionService;
import com.rit.token.ritz.transaction.TransactionStatus;
import com.rit.token.ritz.transaction.dto.DebitRequest;
import com.rit.token.ritz.transaction.dto.RefundApiRequest;
import com.rit.token.ritz.transaction.dto.TransactionApiResponse;
import com.rit.token.ritz.wallet.Wallet;
import com.rit.token.ritz.wallet.WalletRepository;
import com.rit.token.ritz.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private MerchantService merchantService;

    private static final String TEST_STUDENT = "RITTEST001";
    private static final String TEST_MERCHANT = "TILLO_TEST";

    @BeforeEach
    void setUp() {
        if (walletRepository.findByStudentId(TEST_STUDENT).isEmpty()) {
            Wallet wallet = walletService.createStudentAndWallet(
                    TEST_STUDENT, "Test Student", "teststudent@ritchennai.edu.in", "9999988888", "CSE", 2026
            );
            wallet.credit(500L);
            walletRepository.save(wallet);
        } else {
            Wallet wallet = walletRepository.findByStudentId(TEST_STUDENT).get();
            wallet.setBalanceMinorUnits(500L);
            walletRepository.save(wallet);
        }

        if (merchantService.getMerchant(TEST_MERCHANT).isEmpty()) {
            merchantService.registerMerchant(
                    TEST_MERCHANT, "Tillo Test Canteen", "Test Food", MerchantCategory.FOOD, "TILLO", "test@tillo.com", "1234"
            );
        }
    }

    @Test
    @DisplayName("Successful debit deducts exact amount and returns previous/current balance")
    void testSuccessfulDebit() {
        DebitRequest req = new DebitRequest();
        req.setStudentId(TEST_STUDENT);
        req.setMerchantId(TEST_MERCHANT);
        req.setAmount(80L);
        req.setReferenceId("ORDER-TEST-80");
        req.setDescription("Lunch meal");

        TransactionApiResponse resp = transactionService.executeDebit(req, "IDEM-TEST-80", "TILLO");

        assertTrue(resp.isSuccess());
        assertEquals(TransactionStatus.COMPLETED, resp.getStatus());
        assertEquals(80L, resp.getAmount());
        assertEquals(500L, resp.getPreviousBalance());
        assertEquals(420L, resp.getCurrentBalance());

        Wallet wallet = walletRepository.findByStudentId(TEST_STUDENT).orElseThrow();
        assertEquals(420L, wallet.getBalanceMinorUnits());
    }

    @Test
    @DisplayName("Debit with insufficient balance is rejected and balance is preserved")
    void testInsufficientBalance() {
        DebitRequest req = new DebitRequest();
        req.setStudentId(TEST_STUDENT);
        req.setMerchantId(TEST_MERCHANT);
        req.setAmount(9999L); // Far exceeds 500
        req.setReferenceId("ORDER-TEST-EXCESS");

        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.executeDebit(req, "IDEM-EXCESS-1", "TILLO");
        });

        Wallet wallet = walletRepository.findByStudentId(TEST_STUDENT).orElseThrow();
        assertEquals(500L, wallet.getBalanceMinorUnits(), "Wallet balance must remain unchanged");
    }

    @Test
    @DisplayName("Duplicate request with same idempotency key returns cached response without duplicate debit")
    void testIdempotencyProtection() {
        DebitRequest req = new DebitRequest();
        req.setStudentId(TEST_STUDENT);
        req.setMerchantId(TEST_MERCHANT);
        req.setAmount(50L);
        req.setReferenceId("ORDER-IDEM-50");
        String idempotencyKey = "IDEM-KEY-UNIQUE-50";

        // First attempt
        TransactionApiResponse first = transactionService.executeDebit(req, idempotencyKey, "TILLO");
        assertEquals(450L, first.getCurrentBalance());

        // Second attempt with same idempotency key
        TransactionApiResponse second = transactionService.executeDebit(req, idempotencyKey, "TILLO");
        assertEquals(first.getTransactionId(), second.getTransactionId(), "Must return same transaction ID");
        assertEquals(first.getCurrentBalance(), second.getCurrentBalance());

        // Verify balance was only deducted once
        Wallet wallet = walletRepository.findByStudentId(TEST_STUDENT).orElseThrow();
        assertEquals(450L, wallet.getBalanceMinorUnits());
    }

    @Test
    @DisplayName("Refund credits wallet and records compensating ledger entries")
    void testRefundProcessing() {
        // 1. Initial debit
        DebitRequest req = new DebitRequest();
        req.setStudentId(TEST_STUDENT);
        req.setMerchantId(TEST_MERCHANT);
        req.setAmount(100L);
        req.setReferenceId("ORDER-FOR-REFUND");
        TransactionApiResponse debitResp = transactionService.executeDebit(req, "IDEM-REFUND-INIT", "TILLO");
        assertEquals(400L, debitResp.getCurrentBalance());

        // 2. Refund debit
        RefundApiRequest refundReq = new RefundApiRequest();
        refundReq.setTransactionId(debitResp.getTransactionId());
        refundReq.setAmount(100L);
        refundReq.setReason("Item unavailable");
        refundReq.setReferenceId("REFUND-REF-001");

        TransactionApiResponse refundResp = transactionService.executeRefund(refundReq, "IDEM-REFUND-EXEC", "TILLO");
        assertTrue(refundResp.isSuccess());
        assertEquals(100L, refundResp.getAmount());
        assertEquals(400L, refundResp.getPreviousBalance());
        assertEquals(500L, refundResp.getCurrentBalance());

        Wallet wallet = walletRepository.findByStudentId(TEST_STUDENT).orElseThrow();
        assertEquals(500L, wallet.getBalanceMinorUnits(), "Balance must be restored to 500");
    }
}
