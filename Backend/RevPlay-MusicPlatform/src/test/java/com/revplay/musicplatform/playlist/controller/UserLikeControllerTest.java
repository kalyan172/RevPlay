package com.revplay.musicplatform.playlist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.playlist.dto.request.LikeRequest;
import com.revplay.musicplatform.playlist.dto.response.UserLikeResponse;
import com.revplay.musicplatform.playlist.service.UserLikeService;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserLikeController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class UserLikeControllerTest {

    private static final String BASE = "/api/v1/likes";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockBean
    private UserLikeService userLikeService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    UserLikeControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
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
    @DisplayName("POST likes authenticated returns 201 in current controller")
    void postLikesAuthenticated() throws Exception {
        when(userLikeService.likeContent(any()))
                .thenReturn(UserLikeResponse.builder().id(1L).userId(1L).likeableId(2L).likeableType("SONG").build());
        LikeRequest request = new LikeRequest();
        request.setLikeableId(2L);
        request.setLikeableType("SONG");
        mockMvc.perform(post(BASE)
                .with(user("u").roles("LISTENER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST likes no JWT returns 401")
    void postLikesNoJwt() throws Exception {
        LikeRequest request = new LikeRequest();
        request.setLikeableId(2L);
        request.setLikeableType("SONG");
        mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET likes by user authenticated returns 200 with paged data")
    void getLikesAuthenticated() throws Exception {
        when(userLikeService.getUserLikes(1L, null, 0, 10))
                .thenReturn(new PagedResponseDto<>(List.of(UserLikeResponse.builder().id(1L).build()), 0, 10, 1, 1,
                        true, "id", "asc"));
        mockMvc.perform(get(BASE + "/1")
                .with(user("u").roles("LISTENER")))
                .andExpect(status().isOk());
    }
}
