package com.rit.token.ritz.repository;

import com.rit.token.ritz.entity.TokenWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface TokenWalletRepository extends JpaRepository<TokenWallet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM TokenWallet w WHERE w.userId = :userId")
    Optional<TokenWallet> findByUserIdWithLock(@Param("userId") Long userId);

    Optional<TokenWallet> findByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM TokenWallet w")
    BigDecimal getTotalCirculation();

    @Query("SELECT COUNT(w) FROM TokenWallet w WHERE w.balance > 0")
    long countActiveWallets();
}
