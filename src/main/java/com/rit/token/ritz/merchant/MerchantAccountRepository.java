package com.rit.token.ritz.merchant;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantAccountRepository extends JpaRepository<MerchantAccount, Long> {

    Optional<MerchantAccount> findByMerchantId(String merchantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MerchantAccount m WHERE m.merchantId = :merchantId")
    Optional<MerchantAccount> findWithLockByMerchantId(@Param("merchantId") String merchantId);

    @Query("SELECT COALESCE(SUM(m.pendingSettlementMinorUnits), 0) FROM MerchantAccount m")
    Long getTotalPendingSettlements();
}
