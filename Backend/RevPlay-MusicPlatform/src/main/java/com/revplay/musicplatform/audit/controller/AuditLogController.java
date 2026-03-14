package com.revplay.musicplatform.audit.controller;

import com.revplay.musicplatform.audit.dto.request.AuditLogRequest;
import com.revplay.musicplatform.common.constants.ApiPaths;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.audit.dto.response.AuditLogResponse;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.audit.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.revplay.musicplatform.exception.AccessDeniedException;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Query audit logs (admin) and internal trigger endpoint for other roles")
public class AuditLogController {

    private final AuditLogService auditLogService;
    @Value("${app.audit.internal-api-key}")
    private String internalApiKey;

    @GetMapping
    @Operation(summary = "Query audit logs with optional filters (admin only)", description = "Filter by action, user, entity type, and date range. All filters are optional.")
    public ResponseEntity<ApiResponse<PagedResponseDto<AuditLogResponse>>> queryAuditLogs(
            @RequestParam(required = false) @Parameter(description = "e.g. PLAYLIST_DELETED, ROLE_CHANGED") String action,
            @RequestParam(required = false) @Parameter(description = "Filter by user ID who performed action") Long user,
            @RequestParam(required = false) @Parameter(description = "e.g. PLAYLIST, SONG, USER") String entity,
            @RequestParam(required = false) @Parameter(description = "Start date yyyy-MM-dd") String from,
            @RequestParam(required = false) @Parameter(description = "End date yyyy-MM-dd") String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponseDto<AuditLogResponse> response = auditLogService.queryAuditLogs(
                action, user, entity, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved", response));
    }

    @PostMapping("/internal")
    @Operation(summary = "Internal: Record an audit event (called by other roles)", description = "Used by Role 1, 2, 3, 4 to record critical actions in the audit log.")
    public ResponseEntity<ApiResponse<AuditLogResponse>> recordAuditLog(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedKey,
            @Valid @RequestBody AuditLogRequest request) {
        validateInternalKey(providedKey);

        AuditLogResponse response = auditLogService.logAction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Audit log recorded", response));
    }

    private void validateInternalKey(String providedKey) {
        if (providedKey == null || providedKey.isBlank() || !providedKey.equals(internalApiKey)) {
            throw new AccessDeniedException("Invalid internal API key");
        }
    }
}
