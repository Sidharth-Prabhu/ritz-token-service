package com.rit.token.ritz;

import com.rit.token.ritz.merchant.MerchantCategory;
import com.rit.token.ritz.merchant.MerchantService;
import com.rit.token.ritz.transaction.TransactionService;
import com.rit.token.ritz.transaction.dto.DebitRequest;
import com.rit.token.ritz.wallet.Wallet;
import com.rit.token.ritz.wallet.WalletRepository;
import com.rit.token.ritz.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class ConcurrencyDebitTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private MerchantService merchantService;

    private static final String CONCURRENT_STUDENT = "RITCONCURRENT01";
    private static final String CONCURRENT_MERCHANT = "TILLO_CONCURRENT";

    @BeforeEach
    void setUp() {
        if (walletRepository.findByStudentId(CONCURRENT_STUDENT).isEmpty()) {
            Wallet wallet = walletService.createStudentAndWallet(
                    CONCURRENT_STUDENT, "Concurrent Tester", "concurrent@ritchennai.edu.in", "9123456780", "IT", 2026
            );
            wallet.credit(100L); // Exactly 100 tokens
            walletRepository.save(wallet);
        } else {
            Wallet wallet = walletRepository.findByStudentId(CONCURRENT_STUDENT).get();
            wallet.setBalanceMinorUnits(100L);
            walletRepository.save(wallet);
        }

        if (merchantService.getMerchant(CONCURRENT_MERCHANT).isEmpty()) {
            merchantService.registerMerchant(
                    CONCURRENT_MERCHANT, "Concurrent Canteen", "Testing Canteen", MerchantCategory.FOOD, "TILLO", "con@tillo.com", "999"
            );
        }
    }

    @Test
    @DisplayName("Concurrent debits never overspend a wallet (No race condition / double-spend)")
    void testConcurrentDebitsNeverOverspend() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 10 concurrent requests of 20 tokens each. Balance is 100.
        // At most 5 can succeed (5 * 20 = 100). Remaining 5 MUST fail due to insufficient funds!
        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    latch.await(); // Synchronize all threads to start at the exact same instant
                    DebitRequest req = new DebitRequest();
                    req.setStudentId(CONCURRENT_STUDENT);
                    req.setMerchantId(CONCURRENT_MERCHANT);
                    req.setAmount(20L);
                    req.setReferenceId("CONCURRENT-ORD-" + index);

                    transactionService.executeDebit(req, "IDEM-CONCURRENT-" + UUID.randomUUID(), "TILLO");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });
        }

        latch.countDown(); // Fire all threads simultaneously
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(finished, "Threads must complete within timeout");

        Wallet finalWallet = walletRepository.findByStudentId(CONCURRENT_STUDENT).orElseThrow();

        assertEquals(5, successCount.get(), "Exactly 5 debits of 20 tokens can succeed from a 100 token balance");
        assertEquals(5, failureCount.get(), "Remaining 5 debits must be rejected");
        assertEquals(0L, finalWallet.getBalanceMinorUnits(), "Balance must be exactly 0, never negative");
    }
}
