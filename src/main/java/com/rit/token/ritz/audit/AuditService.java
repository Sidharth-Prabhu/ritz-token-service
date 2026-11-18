package com.rit.token.ritz.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void logAction(String actorId, String actorRole, String action, String resourceType, String resourceId, String ipAddress, String details) {
        try {
            AuditLog auditLog = new AuditLog(actorId, actorRole, action, resourceType, resourceId, ipAddress, details);
            repository.save(auditLog);
            log.info("AUDIT: Actor={} Role={} Action={} Resource={}/{}", actorId, actorRole, action, resourceType, resourceId);
        } catch (Exception e) {
            log.error("Failed to record audit log: {}", e.getMessage());
        }
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
