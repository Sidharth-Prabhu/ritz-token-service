package com.rit.token.ritz.service;

import com.rit.token.ritz.dto.StatsResponse;
import com.rit.token.ritz.entity.TokenTransaction;
import com.rit.token.ritz.entity.TokenUnit;
import com.rit.token.ritz.entity.TokenWallet;
import com.rit.token.ritz.repository.TokenTransactionRepository;
import com.rit.token.ritz.repository.TokenUnitRepository;
import com.rit.token.ritz.repository.TokenWalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RitzTokenService {

    private static final Logger log = LoggerFactory.getLogger(RitzTokenService.class);
    private static final BigDecimal MAX_TRANSACTION_LIMIT = new BigDecimal("10000");

    @Autowired
    private TokenWalletRepository walletRepository;

    @Autowired
    private TokenTransactionRepository transactionRepository;

    @Autowired
    private TokenUnitRepository tokenUnitRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public TokenWallet getOrCreateWallet(Long userId, String userName, String mobileNumber) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            TokenWallet newWallet = new TokenWallet(userId, userName, mobileNumber, BigDecimal.ZERO);
            return walletRepository.save(newWallet);
        });
    }

    public Optional<TokenWallet> getWallet(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    public BigDecimal getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(TokenWallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    public List<TokenWallet> getAllWallets() {
        return walletRepository.findAll();
    }

    public List<TokenTransaction> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public Page<TokenTransaction> getTransactions(Long userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    public List<TokenTransaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByTimestampDesc();
    }

    public Page<TokenTransaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAllByOrderByTimestampDesc(pageable);
    }

    public StatsResponse getGlobalStats() {
        BigDecimal totalCirculation = walletRepository.getTotalCirculation();
        long activeWallets = walletRepository.countActiveWallets();
        long totalUsers = walletRepository.count();
        long serializedUnitsTotal = tokenUnitRepository.count();
        long activeUnits = tokenUnitRepository.countByStatus(TokenUnit.TokenStatus.ACTIVE);

        return new StatsResponse(totalCirculation, activeWallets, totalUsers, serializedUnitsTotal, activeUnits);
    }

    public Page<TokenUnit> getCirculation(Pageable pageable) {
        return tokenUnitRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public TokenWallet topUp(Long userId, String userName, String mobileNumber, BigDecimal amount, String paymentRef, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (amount.compareTo(MAX_TRANSACTION_LIMIT) > 0) {
            throw new IllegalArgumentException("Single transaction limit exceeded (Max: " + MAX_TRANSACTION_LIMIT + " Ritz Tokens)");
        }

        // Lock or create wallet
        TokenWallet wallet = walletRepository.findByUserIdWithLock(userId).orElse(null);
        if (wallet == null) {
            wallet = new TokenWallet(userId, userName, mobileNumber, BigDecimal.ZERO);
            wallet = walletRepository.save(wallet);
        } else {
            if (userName != null && !userName.isBlank()) wallet.setUserName(userName);
            if (mobileNumber != null && !mobileNumber.isBlank()) wallet.setMobileNumber(mobileNumber);
        }

        int tokenCount = amount.intValue();
        log.info("MINTING: Serializing {} Ritz tokens for user {}", tokenCount, userId);

        // High-Performance Batch Insertion of serialized units
        if (tokenCount > 0) {
            String sql = "INSERT INTO token_units (token_hash, owner_id, status, created_at) VALUES (?, ?, 'ACTIVE', ?)";
            List<Object[]> batchArgs = new ArrayList<>(tokenCount);
            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < tokenCount; i++) {
                String hash = generateSecureHash();
                batchArgs.add(new Object[]{hash, userId, now});
            }

            jdbcTemplate.batchUpdate(sql, batchArgs);
        }

        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());
        wallet = walletRepository.save(wallet);

        String txDesc = description != null && !description.isBlank() 
                ? description 
                : "Wallet Top Up (Ref: " + paymentRef + ")";

        TokenTransaction transaction = new TokenTransaction(
                userId,
                wallet.getUserName(),
                wallet.getMobileNumber(),
                amount,
                TokenTransaction.TransactionType.TOPUP,
                txDesc,
                paymentRef,
                newBalance
        );
        transactionRepository.save(transaction);

        return wallet;
    }

    @Transactional
    public TokenWallet spend(Long userId, BigDecimal amount, String orderRef, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        TokenWallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for user: " + userId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("INSUFFICIENT_TOKENS: User balance " + wallet.getBalance() + " is less than requested " + amount);
        }

        int amountToSpend = amount.intValue();

        // Identify and burn/delete individual units
        if (amountToSpend > 0) {
            List<TokenUnit> unitsToSpend = tokenUnitRepository.findActiveUnits(userId, amountToSpend);
            if (unitsToSpend.size() < amountToSpend) {
                log.warn("Active serialized units count ({}) less than requested spend ({}) for user {}. Adjusting...", unitsToSpend.size(), amountToSpend, userId);
            }

            if (!unitsToSpend.isEmpty()) {
                List<Long> unitIds = unitsToSpend.stream().map(TokenUnit::getId).collect(Collectors.toList());
                String placeholders = Collections.nCopies(unitIds.size(), "?").stream().collect(Collectors.joining(","));
                String deleteSql = "DELETE FROM token_units WHERE id IN (" + placeholders + ")";
                jdbcTemplate.update(deleteSql, unitIds.toArray());
            }
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());
        wallet = walletRepository.save(wallet);

        String txDesc = description != null && !description.isBlank()
                ? description
                : "Payment for Order " + orderRef;

        TokenTransaction transaction = new TokenTransaction(
                userId,
                wallet.getUserName(),
                wallet.getMobileNumber(),
                amount,
                TokenTransaction.TransactionType.SPEND,
                txDesc,
                orderRef,
                newBalance
        );
        transactionRepository.save(transaction);
        log.info("SPENT: {} Ritz tokens for user {} [Order: {}]", amount, userId, orderRef);

        return wallet;
    }

    @Transactional
    public TokenWallet refund(Long userId, BigDecimal amount, String orderRef, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        TokenWallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for refund: " + userId));

        int tokenCount = amount.intValue();
        log.info("REFUNDING: Reminting {} Ritz tokens for user {} [Order: {}]", tokenCount, userId, orderRef);

        if (tokenCount > 0) {
            String sql = "INSERT INTO token_units (token_hash, owner_id, status, created_at) VALUES (?, ?, 'ACTIVE', ?)";
            List<Object[]> batchArgs = new ArrayList<>(tokenCount);
            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < tokenCount; i++) {
                batchArgs.add(new Object[]{generateSecureHash(), userId, now});
            }
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }

        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());
        wallet = walletRepository.save(wallet);

        TokenTransaction transaction = new TokenTransaction(
                userId,
                wallet.getUserName(),
                wallet.getMobileNumber(),
                amount,
                TokenTransaction.TransactionType.REFUND,
                "Order Refund: " + (reason != null ? reason : "Order Cancelled") + " (Ref: " + orderRef + ")",
                "REF-" + orderRef,
                newBalance
        );
        transactionRepository.save(transaction);

        return wallet;
    }

    private String generateSecureHash() {
        try {
            String base = UUID.randomUUID().toString() + System.nanoTime();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
