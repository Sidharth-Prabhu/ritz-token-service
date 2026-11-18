package com.rit.token.ritz.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;

    public LedgerService(LedgerAccountRepository accountRepository, LedgerEntryRepository entryRepository) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
    }

    public LedgerAccount getOrCreateAccount(String accountNumber, String accountName, AccountType accountType) {
        return accountRepository.findByAccountNumber(accountNumber).orElseGet(() -> {
            LedgerAccount newAccount = new LedgerAccount(accountNumber, accountName, accountType);
            return accountRepository.save(newAccount);
        });
    }

    /**
     * Records a balanced double-entry transaction.
     * Concurrency: Pessimistic write locking is acquired on both accounts in deterministic order
     * to eliminate deadlock possibilities.
     */
    @Transactional
    public void recordDoubleEntry(String transactionId, String debitAccountNumber, String creditAccountNumber, Long amount, String description) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be strictly positive");
        }
        if (debitAccountNumber.equals(creditAccountNumber)) {
            throw new IllegalArgumentException("Debit and Credit accounts must be distinct in double-entry bookkeeping");
        }

        // Lock accounts in lexicographical order to prevent database deadlocks
        String first = debitAccountNumber.compareTo(creditAccountNumber) < 0 ? debitAccountNumber : creditAccountNumber;
        String second = debitAccountNumber.compareTo(creditAccountNumber) < 0 ? creditAccountNumber : debitAccountNumber;

        LedgerAccount firstAcc = accountRepository.findWithLockByAccountNumber(first)
                .orElseThrow(() -> new IllegalArgumentException("Ledger account not found: " + first));
        LedgerAccount secondAcc = accountRepository.findWithLockByAccountNumber(second)
                .orElseThrow(() -> new IllegalArgumentException("Ledger account not found: " + second));

        LedgerAccount debitAccount = first.equals(debitAccountNumber) ? firstAcc : secondAcc;
        LedgerAccount creditAccount = first.equals(creditAccountNumber) ? firstAcc : secondAcc;

        // Apply debit
        debitAccount.applyDebit(amount);
        accountRepository.save(debitAccount);

        String debitEntryId = "ENT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        LedgerEntry debitEntry = new LedgerEntry(
                debitEntryId,
                transactionId,
                debitAccountNumber,
                EntryType.DEBIT,
                amount,
                debitAccount.getBalanceMinorUnits(),
                description != null ? description : "Debit entry"
        );
        entryRepository.save(debitEntry);

        // Apply credit
        creditAccount.applyCredit(amount);
        accountRepository.save(creditAccount);

        String creditEntryId = "ENT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        LedgerEntry creditEntry = new LedgerEntry(
                creditEntryId,
                transactionId,
                creditAccountNumber,
                EntryType.CREDIT,
                amount,
                creditAccount.getBalanceMinorUnits(),
                description != null ? description : "Credit entry"
        );
        entryRepository.save(creditEntry);

        log.info("Double-entry recorded: Txn={} | Debit: {} (Bal={}) | Credit: {} (Bal={}) | Amount={}",
                transactionId, debitAccountNumber, debitAccount.getBalanceMinorUnits(),
                creditAccountNumber, creditAccount.getBalanceMinorUnits(), amount);
    }

    /**
     * Financial audit verification check: ensures total system debits == total system credits.
     */
    public boolean verifyLedgerBalance() {
        Long totalDebits = entryRepository.getTotalDebitVolume();
        Long totalCredits = entryRepository.getTotalCreditVolume();
        boolean balanced = totalDebits.equals(totalCredits);
        if (!balanced) {
            log.error("LEDGER INTEGRITY FAILURE: Total Debits ({}) != Total Credits ({})", totalDebits, totalCredits);
        }
        return balanced;
    }
}
