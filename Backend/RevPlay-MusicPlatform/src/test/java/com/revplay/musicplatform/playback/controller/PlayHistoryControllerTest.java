package com.revplay.musicplatform.playback.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.playback.dto.response.PlayHistoryResponse;
import com.revplay.musicplatform.playback.service.PlayHistoryService;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.user.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlayHistoryController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class PlayHistoryControllerTest {

    private static final String BASE_PATH = "/api/v1/play-history";
    private static final Long USER_ID = 1L;

    private final MockMvc mockMvc;

    @MockBean
    private PlayHistoryService playHistoryService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Autowired
    PlayHistoryControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(
                    invocation.getArgument(0, ServletRequest.class),
                    invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    @DisplayName("POST track play authenticated returns 201")
    void trackPlayAuthenticated() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/track")
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"songId\":9}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST track play without jwt returns 401")
    void trackPlayNoJwt() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/track")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"songId\":9}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET play history by user id returns 200")
    void getHistoryOwnUser() throws Exception {
        when(playHistoryService.getHistory(USER_ID)).thenReturn(
                List.of(new PlayHistoryResponse(1L, USER_ID, 9L, null, Instant.now(), true, 100)));

        mockMvc.perform(get(BASE_PATH + "/" + USER_ID).with(authentication(auth())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE play history by user id returns 204")
    void clearHistoryOwnUser() throws Exception {
        when(playHistoryService.clearHistory(any())).thenReturn(2L);

        mockMvc.perform(delete(BASE_PATH + "/" + USER_ID).with(authentication(auth())))
                .andExpect(status().isNoContent());
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(USER_ID, "listener", UserRole.LISTENER),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));
    }
}
