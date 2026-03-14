package com.revplay.musicplatform.playback.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.playback.dto.request.QueueReorderRequest;
import com.revplay.musicplatform.playback.dto.response.QueueItemResponse;
import com.revplay.musicplatform.playback.exception.PlaybackNotFoundException;
import com.revplay.musicplatform.playback.service.QueueService;
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

@WebMvcTest(QueueController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class QueueControllerTest {

    private static final String BASE_PATH = "/api/v1/queue";
    private static final Long USER_ID = 1L;
    private static final Long QUEUE_ID = 5L;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockBean
    private QueueService queueService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Autowired
    QueueControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
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
    @DisplayName("POST queue valid authenticated request returns 201")
    void addQueueValidAuthenticated() throws Exception {
        when(queueService.addToQueue(any()))
                .thenReturn(new QueueItemResponse(QUEUE_ID, USER_ID, 11L, null, 1, Instant.now()));

        mockMvc.perform(post(BASE_PATH)
                .with(authentication(auth(UserRole.LISTENER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"songId\":11}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST queue with both songId and episodeId returns 400")
    void addQueueBothSongAndEpisode() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                .with(authentication(auth(UserRole.LISTENER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"songId\":11,\"episodeId\":22}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST queue with neither songId nor episodeId returns 400")
    void addQueueNeitherSongNorEpisode() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                .with(authentication(auth(UserRole.LISTENER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST queue without jwt returns 401")
    void addQueueNoJwt() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"songId\":11}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET queue by user id authenticated returns 200")
    void getQueueAuthenticated() throws Exception {
        when(queueService.getQueue(USER_ID))
                .thenReturn(List.of(new QueueItemResponse(QUEUE_ID, USER_ID, 11L, null, 1, Instant.now())));

        mockMvc.perform(get(BASE_PATH + "/" + USER_ID).with(authentication(auth(UserRole.LISTENER))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE queue item authenticated returns 204")
    void deleteQueueItemAuthenticated() throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/" + QUEUE_ID).with(authentication(auth(UserRole.LISTENER))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE queue item not found returns 404")
    void deleteQueueItemNotFound() throws Exception {
        doThrow(new PlaybackNotFoundException("Queue item 5 not found")).when(queueService).removeFromQueue(QUEUE_ID);

        mockMvc.perform(delete(BASE_PATH + "/" + QUEUE_ID).with(authentication(auth(UserRole.LISTENER))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT queue reorder valid request returns 200")
    void reorderQueueValid() throws Exception {
        QueueReorderRequest request = new QueueReorderRequest(USER_ID, List.of(QUEUE_ID));
        when(queueService.reorder(eq(request)))
                .thenReturn(List.of(new QueueItemResponse(QUEUE_ID, USER_ID, 11L, null, 1, Instant.now())));

        mockMvc.perform(put(BASE_PATH + "/reorder")
                .with(authentication(auth(UserRole.LISTENER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private UsernamePasswordAuthenticationToken auth(UserRole role) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(USER_ID, "user", role),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }
}
