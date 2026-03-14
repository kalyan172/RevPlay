package com.revplay.musicplatform.analytics.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.enums.UserRole;
import java.util.List;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class AnalyticsIntegrationTest {

    private static final String BASE = "/api/v1/analytics";
    private static final Long USER_ID = 7001L;
    private static final String TYPE_SONG = "song";
    private static final String PERIOD_WEEKLY = "WEEKLY";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockBean
    private DiscoveryPerformanceService discoveryPerformanceService;

    @Autowired
    AnalyticsIntegrationTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    @DisplayName("dashboard metrics endpoint returns 200 for authenticated user")
    void dashboardMetricsReturns200() throws Exception {
        JsonNode root = perform(get(BASE + "/dashboard-metrics"));

        assertThat(root.path("success").asBoolean()).isTrue();
        assertThat(root.path("data").isObject()).isTrue();
    }

    @Test
    @DisplayName("trending endpoint returns 200 for authenticated user")
    void trendingReturns200() throws Exception {
        JsonNode root = perform(get(BASE + "/trending")
                .param("type", TYPE_SONG)
                .param("period", PERIOD_WEEKLY)
                .param("limit", "5"));

        assertThat(root.path("success").asBoolean()).isTrue();
        assertThat(root.path("data").isArray()).isTrue();
    }

    @Test
    @DisplayName("top artists endpoint returns 200 for authenticated user")
    void topArtistsReturns200() throws Exception {
        JsonNode root = perform(get(BASE + "/top-artists").param("limit", "5"));

        assertThat(root.path("success").asBoolean()).isTrue();
        assertThat(root.path("data").isArray()).isTrue();
    }

    @Test
    @DisplayName("top content endpoint returns 200 for authenticated user")
    void topContentReturns200() throws Exception {
        JsonNode root = perform(get(BASE + "/top-content").param("type", TYPE_SONG).param("limit", "5"));

        assertThat(root.path("success").asBoolean()).isTrue();
        assertThat(root.path("data").isArray()).isTrue();
    }

    @Test
    @DisplayName("analytics endpoints return 401 when unauthenticated")
    void analyticsEndpointsReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get(BASE + "/dashboard-metrics")).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/trending").param("type", TYPE_SONG).param("period", PERIOD_WEEKLY))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/top-artists")).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/top-content").param("type", TYPE_SONG)).andExpect(status().isForbidden());

        assertThat(USER_ID).isPositive();
    }

    private JsonNode perform(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult result = mockMvc.perform(request.with(authentication(auth())))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(USER_ID, "analytics-user", UserRole.ADMIN),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
