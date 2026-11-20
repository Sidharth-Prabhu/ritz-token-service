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

    @Query("SELECT t FROM WalletTransaction t WHERE " +
           "(:type IS NULL OR t.transactionType = :type) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(t.studentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.transactionId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.referenceId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.merchantId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.createdAt DESC")
    Page<WalletTransaction> findFiltered(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amountMinorUnits), 0) FROM WalletTransaction t WHERE t.status = 'COMPLETED' AND t.createdAt >= :since")
    Long getTotalVolumeSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amountMinorUnits), 0) FROM WalletTransaction t WHERE t.transactionType IN ('RECHARGE', 'CREDIT') AND t.status = 'COMPLETED'")
    Long getTotalIssuedVolume();

    @Query("SELECT COALESCE(SUM(t.amountMinorUnits), 0) FROM WalletTransaction t WHERE t.transactionType IN ('RECHARGE', 'CREDIT') AND t.status = 'COMPLETED' AND t.createdAt >= :since")
    Long getIssuedVolumeSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amountMinorUnits), 0) FROM WalletTransaction t WHERE t.transactionType = 'REFUND' AND t.status = 'COMPLETED'")
    Long getTotalRefundVolume();

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
