package com.revplay.musicplatform.audit.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.audit.entity.AdminAuditLog;
import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.audit.repository.AdminAuditLogRepository;
import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.enums.UserRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.audit.internal-api-key=test-internal-key")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class AuditIntegrationTest {

    private static final String BASE = "/api/v1/audit-logs";
    private static final String INTERNAL = BASE + "/internal";
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String API_KEY = "test-internal-key";
    private static final Long ADMIN_USER_ID = 9000L;
    private static final String REQUEST_BODY = """
            {
              "action":"ROLE_CHANGED",
              "performedBy":9000,
              "entityType":"USER",
              "entityId":123,
              "description":"Role updated"
            }
            """;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final AdminAuditLogRepository adminAuditLogRepository;

    @MockBean
    private DiscoveryPerformanceService discoveryPerformanceService;

    @Autowired
    AuditIntegrationTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            AdminAuditLogRepository adminAuditLogRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    @BeforeEach
    void clean() {
        adminAuditLogRepository.deleteAll();
    }

    @Test
    @DisplayName("internal audit endpoint records audit row with valid key")
    void internalEndpointRecordsAudit() throws Exception {
        MvcResult result = mockMvc.perform(post(INTERNAL)
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType("application/json")
                        .content(REQUEST_BODY))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(root.path("success").asBoolean()).isTrue();
        assertThat(adminAuditLogRepository.count()).isEqualTo(1L);
        AdminAuditLog saved = adminAuditLogRepository.findAll().getFirst();
        assertThat(saved.getAction()).isEqualTo(AuditActionType.ROLE_CHANGED);
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.USER);
    }

    @Test
    @DisplayName("internal audit endpoint rejects invalid key")
    void internalEndpointRejectsInvalidKey() throws Exception {
        mockMvc.perform(post(INTERNAL)
                        .header(API_KEY_HEADER, "wrong-key")
                        .contentType("application/json")
                        .content(REQUEST_BODY))
                .andExpect(status().isForbidden());

        assertThat(adminAuditLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("admin can query audit logs with filters")
    void adminCanQueryAuditLogs() throws Exception {
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .action(AuditActionType.ROLE_CHANGED)
                .performedBy(ADMIN_USER_ID)
                .entityType(AuditEntityType.USER)
                .entityId(123L)
                .description("Role updated")
                .build());

        MvcResult result = mockMvc.perform(get(BASE)
                        .with(authentication(authAdmin()))
                        .param("action", "ROLE_CHANGED")
                        .param("entity", "USER")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(root.path("success").asBoolean()).isTrue();
        assertThat(root.path("data").path("content").size()).isEqualTo(1);
    }

    @Test
    @DisplayName("query audit logs requires authentication")
    void queryRequiresAuthentication() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());

        assertThat(adminAuditLogRepository.findAll()).isEmpty();
    }

    private UsernamePasswordAuthenticationToken authAdmin() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(ADMIN_USER_ID, "admin", UserRole.ADMIN),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
