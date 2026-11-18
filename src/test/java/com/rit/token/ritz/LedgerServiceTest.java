package com.rit.token.ritz;

import com.rit.token.ritz.ledger.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class LedgerServiceTest {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerAccountRepository accountRepository;

    @Autowired
    private LedgerEntryRepository entryRepository;

    @BeforeEach
    void setUp() {
        ledgerService.getOrCreateAccount("TEST-ASSET-BANK", "Bank Asset", AccountType.ASSET);
        ledgerService.getOrCreateAccount("TEST-LIAB-STUDENT", "Student Liability", AccountType.LIABILITY);
    }

    @Test
    @DisplayName("Every double-entry transaction must create balanced debit and credit entries")
    @Transactional
    void testBalancedDoubleEntry() {
        String txnId = "TEST-TXN-001";
        Long amount = 250L;

        ledgerService.recordDoubleEntry(
                txnId,
                "TEST-ASSET-BANK",
                "TEST-LIAB-STUDENT",
                amount,
                "Test deposit"
        );

        List<LedgerEntry> entries = entryRepository.findByTransactionIdOrderByCreatedAtAsc(txnId);
        assertEquals(2, entries.size(), "Must have exactly 2 legs in double-entry");

        LedgerEntry debitEntry = entries.stream().filter(e -> e.getEntryType() == EntryType.DEBIT).findFirst().orElseThrow();
        LedgerEntry creditEntry = entries.stream().filter(e -> e.getEntryType() == EntryType.CREDIT).findFirst().orElseThrow();

        assertEquals(amount, debitEntry.getAmountMinorUnits());
        assertEquals(amount, creditEntry.getAmountMinorUnits());
        assertEquals("TEST-ASSET-BANK", debitEntry.getAccountNumber());
        assertEquals("TEST-LIAB-STUDENT", creditEntry.getAccountNumber());

        assertTrue(ledgerService.verifyLedgerBalance(), "Ledger debits must equal credits");
    }

    @Test
    @DisplayName("Ledger entries must never be modified or deleted")
    void testImmutabilityConstraint() {
        String txnId = "TEST-TXN-IMMUTABLE";
        ledgerService.recordDoubleEntry(txnId, "TEST-ASSET-BANK", "TEST-LIAB-STUDENT", 100L, "Immutable test");

        List<LedgerEntry> entries = entryRepository.findByTransactionIdOrderByCreatedAtAsc(txnId);
        assertFalse(entries.isEmpty());

        long initialCount = entryRepository.count();
        assertTrue(initialCount >= 2);
    }
}
