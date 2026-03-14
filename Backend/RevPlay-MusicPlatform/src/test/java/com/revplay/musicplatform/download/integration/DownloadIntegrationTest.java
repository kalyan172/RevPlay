package com.revplay.musicplatform.download.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import com.revplay.musicplatform.download.repository.SongDownloadRepository;
import com.revplay.musicplatform.download.service.SongFileResolver;
import com.revplay.musicplatform.premium.service.SubscriptionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.user.enums.UserRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class DownloadIntegrationTest {

    private static final String DOWNLOAD_PATH_PREFIX = "/api/v1/download/song/";
    private static final String STATUS_PATH = "/api/v1/download/status";
    private static final Long USER_ID = 77L;
    private static final Long ARTIST_ID = 900L;
    private static final Integer DURATION_SECONDS = 180;
    private static final String FILE_URL = "/api/v1/files/songs/demo.mp3";

    private final MockMvc mockMvc;
    private final SongRepository songRepository;
    private final SongDownloadRepository songDownloadRepository;

    private Long songId;

    @MockBean
    private SubscriptionService subscriptionService;
    @MockBean
    private SongFileResolver songFileResolver;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private DiscoveryPerformanceService discoveryPerformanceService;

    @Autowired
    DownloadIntegrationTest(
            MockMvc mockMvc,
            SongRepository songRepository,
            SongDownloadRepository songDownloadRepository
    ) {
        this.mockMvc = mockMvc;
        this.songRepository = songRepository;
        this.songDownloadRepository = songDownloadRepository;
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

        songDownloadRepository.deleteAll();
        songRepository.deleteAll();

        Song song = new Song();
        song.setArtistId(ARTIST_ID);
        song.setTitle("Demo Track");
        song.setDurationSeconds(DURATION_SECONDS);
        song.setFileUrl(FILE_URL);
        Song savedSong = songRepository.save(song);
        songId = savedSong.getSongId();

        when(songFileResolver.loadSongResource(FILE_URL)).thenReturn(new ByteArrayResource("audio".getBytes()));
    }

    @Test
    @DisplayName("premium user download saves one record and returns ok")
    void premiumDownloadSavesRecord() throws Exception {
        when(subscriptionService.isUserPremium(USER_ID)).thenReturn(true);

        mockMvc.perform(get(DOWNLOAD_PATH_PREFIX + songId)
                        .param("userId", USER_ID.toString())
                        .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk());

        assertThat(songDownloadRepository.count()).isEqualTo(1L);
        assertThat(songDownloadRepository.existsByUserIdAndSongId(USER_ID, songId)).isTrue();
    }

    @Test
    @DisplayName("repeated download does not create duplicate records")
    void repeatedDownloadDoesNotDuplicate() throws Exception {
        when(subscriptionService.isUserPremium(USER_ID)).thenReturn(true);

        mockMvc.perform(get(DOWNLOAD_PATH_PREFIX + songId)
                        .param("userId", USER_ID.toString())
                        .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk());

        mockMvc.perform(get(DOWNLOAD_PATH_PREFIX + songId)
                        .param("userId", USER_ID.toString())
                        .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk());

        assertThat(songDownloadRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("download status returns true after successful download")
    void statusReturnsTrueAfterDownload() throws Exception {
        when(subscriptionService.isUserPremium(USER_ID)).thenReturn(true);

        mockMvc.perform(get(DOWNLOAD_PATH_PREFIX + songId)
                        .param("userId", USER_ID.toString())
                        .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk());

        mockMvc.perform(get(STATUS_PATH)
                        .param("userId", USER_ID.toString())
                        .param("songId", songId.toString())
                        .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk());

        assertThat(songDownloadRepository.existsByUserIdAndSongId(USER_ID, songId)).isTrue();
    }

    @Test
    @DisplayName("non premium user download is forbidden")
    void nonPremiumUserCannotDownload() throws Exception {
        when(subscriptionService.isUserPremium(USER_ID)).thenReturn(false);

        mockMvc.perform(get(DOWNLOAD_PATH_PREFIX + songId)
                        .param("userId", USER_ID.toString())
                        .with(authentication(auth(USER_ID))))
                .andExpect(status().isForbidden());

        assertThat(songDownloadRepository.count()).isZero();
    }

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(userId, "user-" + userId, UserRole.LISTENER),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_LISTENER"))
        );
    }
}
