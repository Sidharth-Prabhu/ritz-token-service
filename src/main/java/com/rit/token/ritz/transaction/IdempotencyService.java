package com.rit.token.ritz.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    public String computeHash(String payload) {
        if (payload == null) payload = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public Optional<IdempotencyRecord> findRecord(String idempotencyKey, String scope) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByIdempotencyKeyAndScope(idempotencyKey, scope);
    }

    public IdempotencyRecord saveRecord(String idempotencyKey, String scope, String requestHash, int httpStatus, String responseBody) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        try {
            IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, scope, requestHash, httpStatus, responseBody);
            return repository.save(record);
        } catch (Exception e) {
            log.warn("Failed to save idempotency record: {}", e.getMessage());
            return null;
        }
    }
}
