package com.rit.token.ritz.merchant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantSettlementRepository extends JpaRepository<MerchantSettlement, Long> {
    Optional<MerchantSettlement> findBySettlementId(String settlementId);
    List<MerchantSettlement> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
    Page<MerchantSettlement> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);
    Page<MerchantSettlement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
