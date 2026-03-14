package com.revplay.musicplatform.download.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.download.service.DownloadService;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.security.service.JwtService;
import com.revplay.musicplatform.security.service.TokenRevocationService;
import com.revplay.musicplatform.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DownloadController.class)
@ActiveProfiles("test")
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class DownloadControllerTest {

    private static final String BASE_PATH = "/api/v1/download";
    private static final Long USER_ID = 7L;
    private static final Long SONG_ID = 11L;
    private static final String FILE_NAME = "my-song.mp3";
    private static final String DOWNLOAD_PATH = BASE_PATH + "/song/" + SONG_ID;
    private static final String STATUS_PATH = BASE_PATH + "/status";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DownloadService downloadService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private TokenRevocationService tokenRevocationService;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    @DisplayName("GET download song with authentication returns attachment response")
    void downloadSongWithAuthReturnsAttachment() throws Exception {
        when(downloadService.downloadSong(USER_ID, SONG_ID)).thenReturn(new ByteArrayResource("abc".getBytes()));
        when(downloadService.getDownloadFileName(SONG_ID)).thenReturn(FILE_NAME);

        mockMvc.perform(get(DOWNLOAD_PATH)
                .param("userId", USER_ID.toString())
                .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + FILE_NAME + "\""));

        verify(downloadService).downloadSong(USER_ID, SONG_ID);
        verify(downloadService).getDownloadFileName(SONG_ID);
    }

    @Test
    @DisplayName("GET download song without authentication returns 403")
    void downloadSongWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(DOWNLOAD_PATH).param("userId", USER_ID.toString()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(downloadService);
    }

    @Test
    @DisplayName("GET download status with authentication returns wrapped response")
    void downloadStatusWithAuthReturnsWrappedResponse() throws Exception {
        when(downloadService.isDownloaded(USER_ID, SONG_ID)).thenReturn(true);

        mockMvc.perform(get(STATUS_PATH)
                .param("userId", USER_ID.toString())
                .param("songId", SONG_ID.toString())
                .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isDownloaded").value(true));

        verify(downloadService).isDownloaded(USER_ID, SONG_ID);
    }

    @Test
    @DisplayName("GET download status without authentication returns 403")
    void downloadStatusWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(STATUS_PATH)
                .param("userId", USER_ID.toString())
                .param("songId", SONG_ID.toString()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(downloadService);
    }

    @Test
    @DisplayName("GET download song when service throws not found returns 404")
    void downloadSongNotFoundReturns404() throws Exception {
        when(downloadService.downloadSong(eq(USER_ID), eq(SONG_ID)))
                .thenThrow(new ResourceNotFoundException("Song", SONG_ID));

        mockMvc.perform(get(DOWNLOAD_PATH)
                .param("userId", USER_ID.toString())
                .with(authentication(auth(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(downloadService).downloadSong(USER_ID, SONG_ID);
    }

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(userId, "user-" + userId, UserRole.LISTENER),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));
    }
}
