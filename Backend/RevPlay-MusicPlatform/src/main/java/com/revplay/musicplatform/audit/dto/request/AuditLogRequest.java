package com.revplay.musicplatform.audit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class AuditLogRequest {

    @NotBlank(message = "Action is required")
    private String action;

    @NotNull(message = "performedBy (userId) is required")
    @Positive(message = "performedBy must be a positive number")
    private Long performedBy;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    private Long entityId;

    private String description;
}
