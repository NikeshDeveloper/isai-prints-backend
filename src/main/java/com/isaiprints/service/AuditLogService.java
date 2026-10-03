package com.isaiprints.service;

import com.isaiprints.domain.entity.AuditLog;
import com.isaiprints.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String actorId, String actorType, String action,
                    String entityType, String entityId, String result, String message) {
        AuditLog log = AuditLog.builder()
            .actorId(actorId)
            .actorType(actorType)
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .result(result)
            .message(message)
            .build();
        auditLogRepository.save(log);
    }
}
