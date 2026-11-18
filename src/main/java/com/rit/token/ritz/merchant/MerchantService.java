package com.rit.token.ritz.merchant;

import com.rit.token.ritz.audit.AuditService;
import com.rit.token.ritz.ledger.AccountType;
import com.rit.token.ritz.ledger.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);

    private final MerchantRepository merchantRepository;
    private final MerchantAccountRepository accountRepository;
    private final MerchantSettlementRepository settlementRepository;
    private final LedgerService ledgerService;
    private final AuditService auditService;

    public MerchantService(
            MerchantRepository merchantRepository,
            MerchantAccountRepository accountRepository,
            MerchantSettlementRepository settlementRepository,
            LedgerService ledgerService,
            AuditService auditService) {
        this.merchantRepository = merchantRepository;
        this.accountRepository = accountRepository;
        this.settlementRepository = settlementRepository;
        this.ledgerService = ledgerService;
        this.auditService = auditService;
    }

    @Transactional
    public Merchant registerMerchant(String merchantId, String name, String description, MerchantCategory category, String associatedAppId, String email, String phone) {
        if (merchantRepository.existsByMerchantId(merchantId)) {
            throw new IllegalArgumentException("Merchant ID already registered: " + merchantId);
        }

        Merchant merchant = new Merchant(merchantId, name, description, category, associatedAppId, email, phone);
        Merchant savedMerchant = merchantRepository.save(merchant);

        MerchantAccount account = new MerchantAccount(merchantId);
        accountRepository.save(account);

        // Create double-entry ledger account for merchant
        String accountNumber = "LIAB-MERCH-" + merchantId;
        ledgerService.getOrCreateAccount(accountNumber, "Merchant Payable (" + name + ")", AccountType.LIABILITY);

        auditService.logAction("SYSTEM", "ADMIN", "REGISTER_MERCHANT", "MERCHANT", merchantId, null, "Registered merchant: " + name);
        return savedMerchant;
    }

    public List<Merchant> getAllMerchants() {
        return merchantRepository.findAll();
    }

    public Optional<Merchant> getMerchant(String merchantId) {
        return merchantRepository.findByMerchantId(merchantId);
    }

    public Optional<MerchantAccount> getMerchantAccount(String merchantId) {
        return accountRepository.findByMerchantId(merchantId);
    }

    @Transactional
    public MerchantSettlement requestSettlement(String merchantId, Long amount, String notes, String actorId) {
        MerchantAccount account = accountRepository.findWithLockByMerchantId(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant account not found: " + merchantId));

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Settlement amount must be positive");
        }
        if (account.getPendingSettlementMinorUnits() < amount) {
            throw new IllegalArgumentException("Requested settlement amount (" + amount + ") exceeds available pending settlement (" + account.getPendingSettlementMinorUnits() + ")");
        }

        String settlementId = "SETTLE-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        MerchantSettlement settlement = new MerchantSettlement(settlementId, merchantId, amount, notes);
        MerchantSettlement saved = settlementRepository.save(settlement);

        auditService.logAction(actorId, "FINANCE_ADMIN", "REQUEST_SETTLEMENT", "SETTLEMENT", settlementId, null, "Settlement requested for " + merchantId + ": " + amount + " tokens");
        return saved;
    }

    @Transactional
    public MerchantSettlement processSettlement(String settlementId, String bankReference, String actorId) {
        MerchantSettlement settlement = settlementRepository.findBySettlementId(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));

        if (!"PENDING".equalsIgnoreCase(settlement.getStatus())) {
            throw new IllegalStateException("Settlement is already processed (Status: " + settlement.getStatus() + ")");
        }

        MerchantAccount account = accountRepository.findWithLockByMerchantId(settlement.getMerchantId())
                .orElseThrow(() -> new IllegalStateException("Merchant account not found"));

        account.completeSettlement(settlement.getAmountMinorUnits());
        accountRepository.save(account);

        settlement.setStatus("COMPLETED");
        settlement.setBankReference(bankReference);
        settlement.setSettlementDate(LocalDateTime.now());
        MerchantSettlement saved = settlementRepository.save(settlement);

        // Record double entry:
        // DEBIT: LIAB-MERCH-{merchantId} (Merchant liability decreases)
        // CREDIT: ASSET-BANK-SETTLEMENT (Bank asset decreases)
        String merchantAccNum = "LIAB-MERCH-" + settlement.getMerchantId();
        String bankAssetAccount = "ASSET-BANK-SETTLEMENT";
        ledgerService.getOrCreateAccount(bankAssetAccount, "Bank Settlement Account", AccountType.ASSET);

        ledgerService.recordDoubleEntry(
                settlementId,
                merchantAccNum,
                bankAssetAccount,
                settlement.getAmountMinorUnits(),
                "Bank settlement to " + settlement.getMerchantId() + " (Bank Ref: " + bankReference + ")"
        );

        auditService.logAction(actorId, "FINANCE_ADMIN", "COMPLETE_SETTLEMENT", "SETTLEMENT", settlementId, null, "Completed bank settlement Ref=" + bankReference);
        return saved;
    }
}
