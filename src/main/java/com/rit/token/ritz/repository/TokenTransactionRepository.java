package com.rit.token.ritz.repository;

import com.rit.token.ritz.entity.TokenTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TokenTransactionRepository extends JpaRepository<TokenTransaction, Long> {

    List<TokenTransaction> findByUserIdOrderByTimestampDesc(Long userId);

    Page<TokenTransaction> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    List<TokenTransaction> findAllByOrderByTimestampDesc();

    Page<TokenTransaction> findAllByOrderByTimestampDesc(Pageable pageable);
}
