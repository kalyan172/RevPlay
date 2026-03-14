package com.revplay.musicplatform.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.ai.service.AIService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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

@WebMvcTest(AIController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
@Tag("integration")
class AIControllerTest {

    private static final String BASE_PATH = "/api/ai/chat";

    private final MockMvc mockMvc;

    @MockBean
    private AIService aiService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    AIControllerTest(MockMvc mockMvc) {
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
    @DisplayName("chat with auth returns wrapped AI response")
    void chatWithAuthReturnsResponse() throws Exception {
        when(aiService.getAIResponse("How does premium work?"))
                .thenReturn("Premium removes ads and enables downloads.");

        mockMvc.perform(post(BASE_PATH)
                        .with(user("listener").roles("LISTENER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"How does premium work?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.response").value("Premium removes ads and enables downloads."));

        verify(aiService).getAIResponse("How does premium work?");
    }

    @Test
    @DisplayName("chat without auth returns forbidden")
    void chatWithoutAuthReturnsForbidden() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"How does premium work?"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(aiService);
    }

    @Test
    @DisplayName("blank prompt with auth returns bad request")
    void blankPromptReturnsBadRequest() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                        .with(user("listener").roles("LISTENER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":" "}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiService);
    }
}
