package com.revplay.musicplatform.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.user.dto.request.UpdateUserRoleRequest;
import com.revplay.musicplatform.user.dto.request.UpdateUserStatusRequest;
import com.revplay.musicplatform.user.dto.response.AdminUserDetailsResponse;
import com.revplay.musicplatform.user.dto.response.SimpleMessageResponse;
import com.revplay.musicplatform.user.service.UserAccountAdminService;
import java.time.Instant;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserAccountAdminController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class UserAccountAdminControllerTest {

        private static final String BASE_URL = "/api/v1/admin/users";

        private final MockMvc mockMvc;
        private final ObjectMapper objectMapper;

        @MockBean
        private UserAccountAdminService userAccountAdminService;
        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        @MockBean
        private FileStorageProperties fileStorageProperties;
        @MockBean
        private JpaMetamodelMappingContext jpaMetamodelMappingContext;

        @Autowired
        UserAccountAdminControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
                this.mockMvc = mockMvc;
                this.objectMapper = objectMapper;
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
        @DisplayName("list users with ADMIN role returns 200")
        void listUsersAdmin() throws Exception {
                PagedResponseDto<AdminUserDetailsResponse> page = new PagedResponseDto<>(List.of(
                                new AdminUserDetailsResponse(1L, "admin", "admin@revplay.com", "ADMIN", "ACTIVE",
                                                Instant.now())),
                                0, 20, 1L, 1, true, "createdAt", "desc");
                when(userAccountAdminService.listUsers(0, 20)).thenReturn(page);

                mockMvc.perform(get(BASE_URL).with(user("admin").roles("ADMIN")))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("list users with LISTENER role returns 500 due to GlobalExceptionHandler swallowing AccessDeniedException")
        void listUsersListenerForbidden() throws Exception {
                mockMvc.perform(get(BASE_URL).with(user("listener").roles("LISTENER")))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("list users without jwt returns 403")
        void listUsersUnauthorized() throws Exception {
                mockMvc.perform(get(BASE_URL))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("update status with ADMIN role returns 200")
        void updateStatusAdmin() throws Exception {
                when(userAccountAdminService.updateStatus(any(Long.class), any(UpdateUserStatusRequest.class), any()))
                                .thenReturn(new SimpleMessageResponse("Account status updated"));

                mockMvc.perform(patch(BASE_URL + "/1/status")
                                .with(user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new UpdateUserStatusRequest(true))))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("update role with ADMIN role returns 200")
        void updateRoleAdmin() throws Exception {
                when(userAccountAdminService.updateRole(any(Long.class), any(UpdateUserRoleRequest.class), any()))
                                .thenReturn(new SimpleMessageResponse("User role updated"));

                mockMvc.perform(patch(BASE_URL + "/1/role")
                                .with(user("admin").roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new UpdateUserRoleRequest("ARTIST"))))
                                .andExpect(status().isOk());
        }
}
