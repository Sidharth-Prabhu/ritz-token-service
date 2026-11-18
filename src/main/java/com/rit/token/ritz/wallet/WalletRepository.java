package com.rit.token.ritz.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByStudentId(String studentId);

    Optional<Wallet> findByWalletId(String walletId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.studentId = :studentId")
    Optional<Wallet> findWithLockByStudentId(@Param("studentId") String studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletId = :walletId")
    Optional<Wallet> findWithLockByWalletId(@Param("walletId") String walletId);

    @Query("SELECT COALESCE(SUM(w.balanceMinorUnits), 0) FROM Wallet w WHERE w.status = 'ACTIVE'")
    Long getTotalCirculationMinorUnits();

    long countByStatus(WalletStatus status);
}
