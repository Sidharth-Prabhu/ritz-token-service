package com.rit.token.ritz.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByIdempotencyKeyAndScope(String idempotencyKey, String scope);
    boolean existsByIdempotencyKeyAndScope(String idempotencyKey, String scope);
}
