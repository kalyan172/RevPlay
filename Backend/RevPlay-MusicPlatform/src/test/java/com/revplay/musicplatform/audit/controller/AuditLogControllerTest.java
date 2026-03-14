package com.revplay.musicplatform.audit.controller;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.audit.dto.request.AuditLogRequest;
import com.revplay.musicplatform.audit.dto.response.AuditLogResponse;
import com.revplay.musicplatform.audit.service.AuditLogService;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = AuditLogController.class, properties = "app.audit.internal-api-key=test-internal-key")
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
@Tag("integration")
class AuditLogControllerTest {

    private static final String BASE_PATH = "/api/v1/audit-logs";
    private static final String INTERNAL_PATH = BASE_PATH + "/internal";
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_KEY = "test-internal-key";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockBean
    private AuditLogService auditLogService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    AuditLogControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("GET query audit logs with admin auth returns 200")
    void queryAuditLogsAdmin() throws Exception {
        PagedResponseDto<AuditLogResponse> dto = PagedResponseDto.<AuditLogResponse>builder()
                .content(List.of(AuditLogResponse.builder().id(1L).build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
        when(auditLogService.queryAuditLogs(any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(dto);

        mockMvc.perform(get(BASE_PATH)
                        .with(user("admin").roles("ADMIN"))
                        .param("action", "ROLE_CHANGED")
                        .param("user", "1")
                        .param("entity", "USER")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(auditLogService).queryAuditLogs(any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    @DisplayName("GET query audit logs without auth returns 403")
    void queryAuditLogsNoAuth() throws Exception {
        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isForbidden());

        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("POST internal audit with valid key returns 201")
    void recordInternalValidKey() throws Exception {
        when(auditLogService.logAction(any(AuditLogRequest.class))).thenReturn(AuditLogResponse.builder().id(1L).build());

        mockMvc.perform(post(INTERNAL_PATH)
                        .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(auditLogService).logAction(any(AuditLogRequest.class));
    }

    @Test
    @DisplayName("POST internal audit with missing key returns 403")
    void recordInternalMissingKey() throws Exception {
        mockMvc.perform(post(INTERNAL_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("POST internal audit with wrong key returns 403")
    void recordInternalWrongKey() throws Exception {
        mockMvc.perform(post(INTERNAL_PATH)
                        .header(INTERNAL_KEY_HEADER, "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("POST internal audit invalid body returns 400")
    void recordInternalInvalidBody() throws Exception {
        AuditLogRequest invalid = new AuditLogRequest();
        invalid.setAction("");

        mockMvc.perform(post(INTERNAL_PATH)
                        .header(INTERNAL_KEY_HEADER, INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(auditLogService);
    }

    private AuditLogRequest validRequest() {
        AuditLogRequest request = new AuditLogRequest();
        request.setAction("ROLE_CHANGED");
        request.setPerformedBy(1L);
        request.setEntityType("USER");
        request.setEntityId(2L);
        request.setDescription("desc");
        return request;
    }
}
