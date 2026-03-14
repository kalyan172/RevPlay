package com.revplay.musicplatform.audit.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.audit.dto.request.AuditLogRequest;
import com.revplay.musicplatform.audit.entity.AdminAuditLog;
import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AuditLogMapperTest {

    private final AuditLogMapper mapper = new AuditLogMapper();

    @Test
    @DisplayName("toEntity parses action and entity type")
    void toEntityParsesEnums() {
        AuditLogRequest request = new AuditLogRequest();
        request.setAction("profile_update");
        request.setPerformedBy(1L);
        request.setEntityType("user_profile");
        request.setEntityId(2L);
        request.setDescription("updated");

        AdminAuditLog entity = mapper.toEntity(request);

        assertThat(entity.getAction()).isEqualTo(AuditActionType.PROFILE_UPDATE);
        assertThat(entity.getEntityType()).isEqualTo(AuditEntityType.USER_PROFILE);
    }

    @Test
    @DisplayName("toEntity throws for invalid action")
    void toEntityInvalidAction() {
        AuditLogRequest request = new AuditLogRequest();
        request.setAction("bad");
        request.setPerformedBy(1L);
        request.setEntityType("USER");

        assertThatThrownBy(() -> mapper.toEntity(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid audit action: bad");
    }

    @Test
    @DisplayName("toResponse maps entity fields")
    void toResponseMapsFields() {
        AdminAuditLog log = AdminAuditLog.builder()
                .id(1L)
                .action(AuditActionType.ROLE_CHANGED)
                .performedBy(2L)
                .entityType(AuditEntityType.USER)
                .entityId(3L)
                .description("desc")
                .timestamp(LocalDateTime.parse("2026-01-01T00:00:00"))
                .build();

        var response = mapper.toResponse(log);

        assertThat(response.getAction()).isEqualTo("ROLE_CHANGED");
        assertThat(response.getEntityType()).isEqualTo("USER");
    }
}
