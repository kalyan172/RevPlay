package com.revplay.musicplatform.premium.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.premium.dto.PremiumStatusResponse;
import com.revplay.musicplatform.premium.service.SubscriptionService;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.user.enums.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PremiumController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class PremiumControllerTest {

    private static final String BASE_PATH = "/api/v1/premium";
    private static final String UPGRADE_PATH = BASE_PATH + "/upgrade";
    private static final String STATUS_PATH = BASE_PATH + "/status";
    private static final Long USER_ID = 9L;
    private static final Long OTHER_USER_ID = 99L;
    private static final String MONTHLY = "MONTHLY";
    private static final String USERNAME = "premium-user";
    private static final String USER_PARAM = "userId";
    private static final String PLAN_PARAM = "planType";

    private final MockMvc mockMvc;

    @MockBean
    private SubscriptionService subscriptionService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    PremiumControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("POST upgrade with matching authenticated user returns 200")
    void upgradeWithMatchingUserReturns200() throws Exception {
        mockMvc.perform(post(UPGRADE_PATH)
                .param(USER_PARAM, USER_ID.toString())
                .param(PLAN_PARAM, MONTHLY)
                .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk());

        verify(subscriptionService).upgradeToPremium(USER_ID, MONTHLY);
    }

    @Test
    @DisplayName("POST upgrade with mismatched user returns 403")
    void upgradeWithMismatchedUserReturns403() throws Exception {
        mockMvc.perform(post(UPGRADE_PATH)
                .param(USER_PARAM, OTHER_USER_ID.toString())
                .param(PLAN_PARAM, MONTHLY)
                .with(authentication(auth(USER_ID))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(subscriptionService);
    }

    @Test
    @DisplayName("POST upgrade without authentication returns 403")
    void upgradeWithoutAuthenticationReturns403() throws Exception {
        mockMvc.perform(post(UPGRADE_PATH)
                .param(USER_PARAM, USER_ID.toString())
                .param(PLAN_PARAM, MONTHLY)
                .with(anonymous()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subscriptionService);
    }

    @Test
    @DisplayName("GET status with matching authenticated user returns 200")
    void getStatusWithMatchingUserReturns200() throws Exception {
        when(subscriptionService.getPremiumStatus(USER_ID))
                .thenReturn(new PremiumStatusResponse(true, LocalDateTime.now().plusDays(3)));

        mockMvc.perform(get(STATUS_PATH)
                .param(USER_PARAM, USER_ID.toString())
                .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPremium").value(true));

        verify(subscriptionService).getPremiumStatus(USER_ID);
    }

    @Test
    @DisplayName("GET status with mismatched user returns 403")
    void getStatusWithMismatchedUserReturns403() throws Exception {
        mockMvc.perform(get(STATUS_PATH)
                .param(USER_PARAM, OTHER_USER_ID.toString())
                .with(authentication(auth(USER_ID))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subscriptionService);
    }

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(userId, USERNAME, UserRole.LISTENER);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + UserRole.LISTENER.name())));
    }
}
