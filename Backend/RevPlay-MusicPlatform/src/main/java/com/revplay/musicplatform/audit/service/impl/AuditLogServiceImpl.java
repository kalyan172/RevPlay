package com.revplay.musicplatform.audit.service.impl;

import com.revplay.musicplatform.audit.dto.request.AuditLogRequest;
import com.revplay.musicplatform.audit.dto.response.AuditLogResponse;
import com.revplay.musicplatform.audit.entity.AdminAuditLog;
import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.audit.mapper.AuditLogMapper;
import com.revplay.musicplatform.audit.repository.AdminAuditLogRepository;
import com.revplay.musicplatform.audit.service.AuditLogService;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.security.AuthContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AdminAuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final AuthContextUtil authContextUtil;


    @Transactional
    public AuditLogResponse logAction(AuditLogRequest request) {
        AdminAuditLog log = auditLogMapper.toEntity(request);
        AdminAuditLog saved = auditLogRepository.save(log);
        return auditLogMapper.toResponse(saved);
    }


    @Transactional
    public void logInternal(AuditActionType action, Long performedBy, AuditEntityType entityType,
            Long entityId, String description) {
        AdminAuditLog auditLog = AdminAuditLog.builder()
                .action(action)
                .performedBy(performedBy)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build();
        auditLogRepository.save(auditLog);
        log.info("Audit logged: action={}, entity={}/{}", action.name(), entityType.name(), entityId);
    }


    @Transactional(readOnly = true)
    public PagedResponseDto<AuditLogResponse> queryAuditLogs(
            String action, Long performedBy, String entityType,
            String from, String to, int page, int size) {
        authContextUtil.requireAdmin();

        LocalDateTime fromDateTime = from != null
                ? LocalDate.parse(from).atStartOfDay()
                : null;
        LocalDateTime toDateTime = to != null
                ? LocalDate.parse(to).atTime(LocalTime.MAX)
                : null;

        Pageable pageable = PageRequest.of(page, size);
        Page<AdminAuditLog> resultPage = auditLogRepository.findWithFilters(
                parseActionFilter(action),
                performedBy,
                parseEntityTypeFilter(entityType),
                fromDateTime,
                toDateTime,
                pageable);

        Page<AuditLogResponse> responsePage = resultPage.map(auditLogMapper::toResponse);
        return PagedResponseDto.of(responsePage);
    }

    private AuditActionType parseActionFilter(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        try {
            return AuditActionType.valueOf(action.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid audit action filter: " + action);
        }
    }

    private AuditEntityType parseEntityTypeFilter(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            return null;
        }
        try {
            return AuditEntityType.valueOf(entityType.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid audit entity type filter: " + entityType);
        }
    }
}


