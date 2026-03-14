package com.revplay.musicplatform.audit.dto.response;

import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private String action;
    private Long performedBy;
    private String entityType;
    private Long entityId;
    private String description;
    private LocalDateTime timestamp;
}
