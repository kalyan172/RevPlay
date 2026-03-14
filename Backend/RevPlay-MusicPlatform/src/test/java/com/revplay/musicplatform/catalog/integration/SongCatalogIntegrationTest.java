package com.revplay.musicplatform.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.ActiveProfiles;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.enums.ArtistType;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.repository.AlbumRepository;
import com.revplay.musicplatform.catalog.repository.PodcastEpisodeRepository;
import com.revplay.musicplatform.catalog.repository.PodcastRepository;
import com.revplay.musicplatform.catalog.repository.SongGenreRepository;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.catalog.util.AudioMetadataService;
import com.revplay.musicplatform.catalog.util.FileStorageService;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.entity.User;
import com.revplay.musicplatform.user.enums.UserRole;
import com.revplay.musicplatform.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class SongCatalogIntegrationTest {

    private static final String ALBUMS_PATH = "/api/v1/albums";
    private static final String SONGS_PATH = "/api/v1/songs";
    private static final String ARTISTS_PATH = "/api/v1/artists";
    private static final String SEARCH_PATH = "/api/v1/search";
    private static Long artistUserId;
    private static Long listenerUserId;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private SongGenreRepository songGenreRepository;
    @Autowired
    private PodcastRepository podcastRepository;
    @Autowired
    private PodcastEpisodeRepository podcastEpisodeRepository;

    @MockBean
    private FileStorageService fileStorageService;
    @MockBean
    private AudioMetadataService audioMetadataService;
    @MockBean
    private DiscoveryPerformanceService discoveryPerformanceService;
    @SpyBean
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        songGenreRepository.deleteAll();
        songRepository.deleteAll();
        albumRepository.deleteAll();
        podcastEpisodeRepository.deleteAll();
        podcastRepository.deleteAll();
        artistRepository.deleteAll();
        userRepository.deleteAll();
        artistUserId = userRepository.save(user("artist@revplay.com", "artist-user", UserRole.ARTIST)).getUserId();
        listenerUserId = userRepository.save(user("listener@revplay.com", "listener-user", UserRole.LISTENER))
                .getUserId();
        artistRepository.save(artist(artistUserId, "Artist One", ArtistType.MUSIC));
        when(fileStorageService.storeSong(any())).thenReturn("track.mp3");
        when(audioMetadataService.resolveDurationSeconds(any(), anyInt())).thenReturn(180);
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.contains("information_schema.tables")) {
                return 1L;
            }
            return invocation.callRealMethod();
        }).when(jdbcTemplate).queryForObject(anyString(), eq(Long.class), any());
    }

    @Test
    @DisplayName("artist creates album then creates song and song appears in artist list")
    void createAlbumThenSongThenListByArtist() throws Exception {
        Long albumId = createAlbumAsArtist("Album A");
        Long songId = createSongAsArtist("Skyline", albumId);
        Long artistId = artistRepository.findByUserId(artistUserId).orElseThrow().getArtistId();

        MvcResult listResult = mockMvc.perform(get(ARTISTS_PATH + "/" + artistId + "/songs")
                .with(authentication(artistAuthentication())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = root(listResult).path("data").path("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.findValuesAsText("songId")).contains(String.valueOf(songId));
    }

    @Test
    @DisplayName("artist soft deletes song and db reflects isActive false")
    void softDeleteSong() throws Exception {
        Long albumId = createAlbumAsArtist("Album A");
        Long songId = createSongAsArtist("ToDelete", albumId);

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(SONGS_PATH + "/" + songId)
                        .with(authentication(artistAuthentication())))
                .andExpect(status().isOk());

        Song deleted = songRepository.findById(songId).orElseThrow();
        assertThat(deleted.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("listener cannot post song and gets unauthorized from access validator")
    void listenerPostSongDenied() throws Exception {
        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                "application/json",
                "{\"title\":\"LSong\",\"durationSeconds\":180}".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "a.mp3", "audio/mpeg", "abc".getBytes());

        mockMvc.perform(multipart(SONGS_PATH)
                .file(metadata)
                .file(file)
                .with(authentication(listenerAuthentication())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("search finds newly created song by title")
    void searchFindsNewSong() throws Exception {
        Long albumId = createAlbumAsArtist("Album A");
        createSongAsArtist("FindMe", albumId);

        MvcResult searchResult = mockMvc.perform(get(SEARCH_PATH)
                .param("q", "FindMe")
                .param("type", "SONG"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode titles = root(searchResult).path("data").path("content");
        assertThat(titles.toString()).containsIgnoringCase("FindMe");
    }

    @Test
    @DisplayName("get deleted song by id still returns 200")
    void getDeletedSongStillReturns200() throws Exception {
        Long albumId = createAlbumAsArtist("Album A");
        Long songId = createSongAsArtist("SoftDeleted", albumId);
        mockMvc.perform(
                delete(SONGS_PATH + "/" + songId)
                        .with(authentication(artistAuthentication())))
                .andExpect(status().isOk());

        mockMvc.perform(get(SONGS_PATH + "/" + songId)
                .with(authentication(artistAuthentication())))
                .andExpect(status().isOk());
    }

    private Long createAlbumAsArtist(String title) throws Exception {
        MvcResult result = mockMvc.perform(post(ALBUMS_PATH)
                .with(authentication(artistAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return root(result).path("data").path("albumId").asLong();
    }

    private Long createSongAsArtist(String title, Long albumId) throws Exception {
        String metadataJson = "{\"title\":\"" + title + "\",\"durationSeconds\":180,\"albumId\":" + albumId + "}";
        MockMultipartFile metadata = new MockMultipartFile("metadata", "", "application/json", metadataJson.getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "track.mp3", "audio/mpeg", "abc".getBytes());

        MvcResult result = mockMvc.perform(multipart(SONGS_PATH)
                .file(metadata)
                .file(file)
                .with(authentication(artistAuthentication())))
                .andExpect(status().isCreated())
                .andReturn();
        return root(result).path("data").path("songId").asLong();
    }

    private JsonNode root(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UsernamePasswordAuthenticationToken artistAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(artistUserId, "artist-user", UserRole.ARTIST),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ARTIST")));
    }

    private UsernamePasswordAuthenticationToken listenerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(listenerUserId, "listener-user", UserRole.LISTENER),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));
    }

    private User user(String email, String username, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setIsActive(Boolean.TRUE);
        user.setEmailVerified(Boolean.TRUE);
        user.setCreatedAt(Instant.now().minusSeconds(10));
        user.setUpdatedAt(Instant.now().minusSeconds(5));
        return user;
    }

    private Artist artist(Long userId, String displayName, ArtistType type) {
        Artist artist = new Artist();
        artist.setUserId(userId);
        artist.setDisplayName(displayName);
        artist.setArtistType(type);
        artist.setVerified(Boolean.TRUE);
        return artist;
    }
}
