package com.rit.token.ritz.repository;

import com.rit.token.ritz.entity.TokenUnit;
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
public interface TokenUnitRepository extends JpaRepository<TokenUnit, Long> {

    @Query(value = "SELECT * FROM token_units WHERE owner_id = :ownerId AND status = 'ACTIVE' ORDER BY id ASC LIMIT :amount", nativeQuery = true)
    List<TokenUnit> findActiveUnits(@Param("ownerId") Long ownerId, @Param("amount") int amount);

    List<TokenUnit> findByOrderRef(String orderRef);

    long countByOwnerIdAndStatus(Long ownerId, TokenUnit.TokenStatus status);

    long countByStatus(TokenUnit.TokenStatus status);

    Page<TokenUnit> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<TokenUnit> findByTokenHash(String tokenHash);

    List<TokenUnit> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}
