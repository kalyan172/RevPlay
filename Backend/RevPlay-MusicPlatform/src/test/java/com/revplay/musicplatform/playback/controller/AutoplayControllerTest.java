package com.revplay.musicplatform.playback.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.mapper.SongMapper;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.playback.service.AutoplayService;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.user.enums.UserRole;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AutoplayController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class AutoplayControllerTest {

    private static final String PATH = "/api/v1/autoplay/next/1/10";

    private final MockMvc mockMvc;

    @MockBean
    private AutoplayService autoplayService;
    @MockBean
    private SongMapper songMapper;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Autowired
    AutoplayControllerTest(MockMvc mockMvc) {
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
    @DisplayName("GET autoplay next authenticated returns 200")
    void getAutoplayNextAuthenticated() throws Exception {
        Song song = new Song();
        song.setSongId(20L);
        song.setTitle("Next");
        when(autoplayService.getNextSong(anyLong(), anyLong())).thenReturn(song);
        when(songMapper.toResponse(song)).thenReturn(new com.revplay.musicplatform.catalog.dto.response.SongResponse());

        mockMvc.perform(get(PATH).with(authentication(auth())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET autoplay next without jwt returns 401")
    void getAutoplayNextNoJwt() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(1L, "listener", UserRole.LISTENER),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));
    }
}
