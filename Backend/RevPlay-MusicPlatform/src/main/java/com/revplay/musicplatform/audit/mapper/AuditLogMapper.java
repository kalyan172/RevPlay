package com.revplay.musicplatform.audit.mapper;

import com.revplay.musicplatform.audit.dto.request.AuditLogRequest;
import com.revplay.musicplatform.audit.dto.response.AuditLogResponse;
import com.revplay.musicplatform.audit.entity.AdminAuditLog;
import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import org.springframework.stereotype.Component;


@Component
public class AuditLogMapper {


    public AdminAuditLog toEntity(AuditLogRequest request) {
        return AdminAuditLog.builder()
                .action(parseAction(request.getAction()))
                .performedBy(request.getPerformedBy())
                .entityType(parseEntityType(request.getEntityType()))
                .entityId(request.getEntityId())
                .description(request.getDescription())
                .build();
    }


    public AuditLogResponse toResponse(AdminAuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction() != null ? log.getAction().name() : null)
                .performedBy(log.getPerformedBy())
                .entityType(log.getEntityType() != null ? log.getEntityType().name() : null)
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .timestamp(log.getTimestamp())
                .build();
    }

    private AuditActionType parseAction(String action) {
        try {
            return AuditActionType.valueOf(action.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid audit action: " + action);
        }
    }

    private AuditEntityType parseEntityType(String entityType) {
        try {
            return AuditEntityType.valueOf(entityType.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid audit entity type: " + entityType);
        }
    }
}
