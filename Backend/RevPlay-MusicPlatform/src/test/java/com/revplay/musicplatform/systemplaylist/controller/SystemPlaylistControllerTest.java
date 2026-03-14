package com.revplay.musicplatform.systemplaylist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.systemplaylist.dto.response.SystemPlaylistResponse;
import com.revplay.musicplatform.systemplaylist.service.SystemPlaylistService;
import com.revplay.musicplatform.user.enums.UserRole;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemPlaylistController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class SystemPlaylistControllerTest {

        private static final String BASE_PATH = "/api/v1/system-playlists";
        private static final String PLAYLIST_SLUG = "focus-mix";
        private static final Long PLAYLIST_ID = 10L;
        private static final Long USER_ID = 1L;
        private static final Long SONG_ID_1 = 1L;
        private static final Long SONG_ID_2 = 2L;
        private static final Long SONG_ID_3 = 3L;
        private static final String VALID_BODY = "{\"songIds\":[1,2]}";
        private static final String INVALID_EMPTY_BODY = "{\"songIds\":[]}";

        private final MockMvc mockMvc;

        @MockBean
        private SystemPlaylistService systemPlaylistService;
        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        @MockBean
        private FileStorageProperties fileStorageProperties;
        @MockBean
        private JpaMetamodelMappingContext jpaMetamodelMappingContext;

        @Autowired
        SystemPlaylistControllerTest(MockMvc mockMvc) {
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
        @DisplayName("GET all active playlists with authentication returns 200")
        void getAllActivePlaylistsReturns200() throws Exception {
                SystemPlaylistResponse response = SystemPlaylistResponse.builder()
                                .id(PLAYLIST_ID)
                                .name("Focus Mix")
                                .slug(PLAYLIST_SLUG)
                                .description("Coding")
                                .build();
                when(systemPlaylistService.getAllActivePlaylists()).thenReturn(List.of(response));

                mockMvc.perform(get(BASE_PATH).with(authentication(auth())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data[0].slug").value(PLAYLIST_SLUG));

                verify(systemPlaylistService).getAllActivePlaylists();
        }

        @Test
        @DisplayName("GET all active playlists without authentication returns 403")
        void getAllActivePlaylistsWithoutAuthReturns403() throws Exception {
                mockMvc.perform(get(BASE_PATH))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(systemPlaylistService);
        }

        @Test
        @DisplayName("GET playlist songs by slug with authentication returns 200")
        void getSongIdsBySlugReturns200() throws Exception {
                when(systemPlaylistService.getSongIdsBySlug(PLAYLIST_SLUG))
                                .thenReturn(List.of(SONG_ID_1, SONG_ID_2, SONG_ID_3));

                mockMvc.perform(get(BASE_PATH + "/" + PLAYLIST_SLUG + "/songs").with(authentication(auth())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data[0]").value(SONG_ID_1));

                verify(systemPlaylistService).getSongIdsBySlug(PLAYLIST_SLUG);
        }

        @Test
        @DisplayName("GET playlist songs by slug not found returns 404")
        void getSongIdsBySlugNotFoundReturns404() throws Exception {
                when(systemPlaylistService.getSongIdsBySlug(PLAYLIST_SLUG))
                                .thenThrow(new ResourceNotFoundException("System playlist", PLAYLIST_SLUG));

                mockMvc.perform(get(BASE_PATH + "/" + PLAYLIST_SLUG + "/songs").with(authentication(auth())))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success").value(false));

                verify(systemPlaylistService).getSongIdsBySlug(PLAYLIST_SLUG);
        }

        @Test
        @DisplayName("POST add songs with valid body returns 201")
        void addSongsBySlugValidReturns201() throws Exception {
                mockMvc.perform(post(BASE_PATH + "/" + PLAYLIST_SLUG + "/songs")
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true));

                verify(systemPlaylistService).addSongsBySlug(PLAYLIST_SLUG, List.of(SONG_ID_1, SONG_ID_2));
        }

        @Test
        @DisplayName("POST add songs with invalid body returns 400")
        void addSongsBySlugInvalidBodyReturns400() throws Exception {
                mockMvc.perform(post(BASE_PATH + "/" + PLAYLIST_SLUG + "/songs")
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(INVALID_EMPTY_BODY))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));

                verifyNoInteractions(systemPlaylistService);
        }

        @Test
        @DisplayName("POST add songs without authentication returns 403")
        void addSongsBySlugNoAuthReturns403() throws Exception {
                mockMvc.perform(post(BASE_PATH + "/" + PLAYLIST_SLUG + "/songs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(systemPlaylistService);
        }

        private UsernamePasswordAuthenticationToken auth() {
                return new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUserPrincipal(USER_ID, "user", UserRole.LISTENER),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));
        }
}
