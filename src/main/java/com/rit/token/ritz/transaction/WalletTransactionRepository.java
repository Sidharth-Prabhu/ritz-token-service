package com.rit.token.ritz.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Optional<WalletTransaction> findByTransactionId(String transactionId);

    Optional<WalletTransaction> findByReferenceId(String referenceId);

    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

    List<WalletTransaction> findByStudentIdOrderByCreatedAtDesc(String studentId);

    Page<WalletTransaction> findByStudentIdOrderByCreatedAtDesc(String studentId, Pageable pageable);

    List<WalletTransaction> findByMerchantIdOrderByCreatedAtDesc(String merchantId);

    Page<WalletTransaction> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    Page<WalletTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(t) FROM WalletTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amountMinorUnits), 0) FROM WalletTransaction t WHERE t.transactionType = 'RECHARGE' AND t.status = 'COMPLETED'")
    Long getTotalRechargeVolume();

    @Query("SELECT COALESCE(SUM(t.amountMinorUnits), 0) FROM WalletTransaction t WHERE t.transactionType = 'DEBIT' AND t.status = 'COMPLETED'")
    Long getTotalSpendingVolume();

    @Query("SELECT COALESCE(SUM(t.amountMinorUnits), 0) FROM WalletTransaction t WHERE t.merchantId = :merchantId AND t.status = 'COMPLETED' AND t.transactionType = 'DEBIT'")
    Long getMerchantTotalVolume(@Param("merchantId") String merchantId);

    @Query("SELECT COALESCE(SUM(t.amountMinorUnits), 0) FROM WalletTransaction t WHERE t.merchantId = :merchantId AND t.status = 'COMPLETED' AND t.transactionType = 'DEBIT' AND t.createdAt >= :since")
    Long getMerchantVolumeSince(@Param("merchantId") String merchantId, @Param("since") LocalDateTime since);
}
