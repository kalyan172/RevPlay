package com.revplay.musicplatform.audit.service;

import com.revplay.musicplatform.audit.dto.request.AuditLogRequest;
import com.revplay.musicplatform.audit.dto.response.AuditLogResponse;
import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.common.dto.PagedResponseDto;

public interface AuditLogService {

    AuditLogResponse logAction(AuditLogRequest request);

    void logInternal(AuditActionType action, Long performedBy, AuditEntityType entityType, Long entityId, String description);

    PagedResponseDto<AuditLogResponse> queryAuditLogs(
            String action,
            Long performedBy,
            String entityType,
            String from,
            String to,
            int page,
            int size
    );
}