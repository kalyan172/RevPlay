package com.revplay.musicplatform.audit.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.audit.dto.request.AuditLogRequest;
import com.revplay.musicplatform.audit.dto.response.AuditLogResponse;
import com.revplay.musicplatform.audit.entity.AdminAuditLog;
import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.audit.mapper.AuditLogMapper;
import com.revplay.musicplatform.audit.repository.AdminAuditLogRepository;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.security.AuthContextUtil;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuditLogServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long ENTITY_ID = 20L;
    private static final String ACTION = "ROLE_CHANGED";
    private static final String ENTITY_TYPE = "USER";
    private static final String DESCRIPTION = "Role updated";
    private static final String FROM = "2026-01-01";
    private static final String TO = "2026-01-31";
    private static final String FROM_PARSED = "2026-01-01T00:00:00";
    private static final String TO_PARSED = "2026-01-31T23:59:59.999999999";
    private static final int PAGE = 0;
    private static final int SIZE = 20;

    @Mock
    private AdminAuditLogRepository auditLogRepository;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private AuthContextUtil authContextUtil;

    private AuditLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuditLogServiceImpl(auditLogRepository, auditLogMapper, authContextUtil);
    }

    @Test
    @DisplayName("logAction maps request saves entity and maps response")
    void logActionSuccess() {
        AuditLogRequest request = request();
        AdminAuditLog entity = logEntity();
        AuditLogResponse response = response();
        when(auditLogMapper.toEntity(request)).thenReturn(entity);
        when(auditLogRepository.save(entity)).thenReturn(entity);
        when(auditLogMapper.toResponse(entity)).thenReturn(response);

        AuditLogResponse actual = service.logAction(request);

        assertThat(actual).isSameAs(response);
    }

    @Test
    @DisplayName("logInternal builds and saves audit entry")
    void logInternalSuccess() {
        when(auditLogRepository.save(any(AdminAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.logInternal(AuditActionType.ADMIN_ACTION, USER_ID, AuditEntityType.SYSTEM, ENTITY_ID, DESCRIPTION);

        verify(auditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    @DisplayName("queryAuditLogs parses filters and maps page response")
    void queryAuditLogsSuccess() {
        AdminAuditLog entity = logEntity();
        AuditLogResponse mapped = response();
        Page<AdminAuditLog> entityPage = new PageImpl<>(List.of(entity), PageRequest.of(PAGE, SIZE), 1);

        when(auditLogRepository.findWithFilters(
                eq(AuditActionType.ROLE_CHANGED),
                eq(USER_ID),
                eq(AuditEntityType.USER),
                eq(LocalDateTime.parse(FROM_PARSED)),
                eq(LocalDateTime.parse(TO_PARSED)),
                any(Pageable.class))).thenReturn(entityPage);
        when(auditLogMapper.toResponse(entity)).thenReturn(mapped);

        PagedResponseDto<AuditLogResponse> result = service.queryAuditLogs(ACTION, USER_ID, ENTITY_TYPE, FROM, TO, PAGE,
                SIZE);

        assertThat(result.getContent()).containsExactly(mapped);
        assertThat(result.getPage()).isEqualTo(PAGE);
    }

    @Test
    @DisplayName("queryAuditLogs supports null filters")
    void queryAuditLogsWithNullFilters() {
        when(auditLogRepository.findWithFilters(eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(PAGE, SIZE)));

        PagedResponseDto<AuditLogResponse> result = service.queryAuditLogs(null, null, null, null, null, PAGE, SIZE);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("queryAuditLogs throws when action filter is invalid")
    void queryAuditLogsInvalidAction() {
        assertThatThrownBy(() -> service.queryAuditLogs("invalid_action", USER_ID, ENTITY_TYPE, null, null, PAGE, SIZE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid audit action filter");
    }

    @Test
    @DisplayName("queryAuditLogs throws when entity type filter is invalid")
    void queryAuditLogsInvalidEntityType() {
        assertThatThrownBy(() -> service.queryAuditLogs(ACTION, USER_ID, "invalid_entity", null, null, PAGE, SIZE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid audit entity type filter");
    }

    private AuditLogRequest request() {
        AuditLogRequest request = new AuditLogRequest();
        request.setAction(ACTION);
        request.setPerformedBy(USER_ID);
        request.setEntityType(ENTITY_TYPE);
        request.setEntityId(ENTITY_ID);
        request.setDescription(DESCRIPTION);
        return request;
    }

    private AdminAuditLog logEntity() {
        return AdminAuditLog.builder()
                .id(1L)
                .action(AuditActionType.ROLE_CHANGED)
                .performedBy(USER_ID)
                .entityType(AuditEntityType.USER)
                .entityId(ENTITY_ID)
                .description(DESCRIPTION)
                .build();
    }

    private AuditLogResponse response() {
        return AuditLogResponse.builder()
                .id(1L)
                .action(ACTION)
                .performedBy(USER_ID)
                .entityType(ENTITY_TYPE)
                .entityId(ENTITY_ID)
                .description(DESCRIPTION)
                .build();
    }
}
