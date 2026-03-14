package com.revplay.musicplatform.playlist.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.playlist.repository.PlaylistFollowRepository;
import com.revplay.musicplatform.playlist.repository.PlaylistRepository;
import com.revplay.musicplatform.playlist.repository.PlaylistSongRepository;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.enums.UserRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class PlaylistIntegrationTest {

    private static final String BASE = "/api/v1/playlists";
    private static final Long USER_A = 1001L;
    private static final Long USER_B = 1002L;
    private static final Long SONG_1 = 5001L;
    private static final Long SONG_2 = 5002L;
    private static final Long SONG_3 = 5003L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private PlaylistSongRepository playlistSongRepository;
    @Autowired
    private PlaylistFollowRepository playlistFollowRepository;
    @MockBean
    private DiscoveryPerformanceService discoveryPerformanceService;

    @BeforeEach
    void clean() {
        playlistFollowRepository.deleteAll();
        playlistSongRepository.deleteAll();
        playlistRepository.deleteAll();
    }

    @Test
    @DisplayName("create playlist add songs and verify initial order")
    void createAddAndOrder() throws Exception {
        Long playlistId = createPlaylist(USER_A, "Mix A", true);
        addSong(USER_A, playlistId, SONG_1, null);
        addSong(USER_A, playlistId, SONG_2, null);
        addSong(USER_A, playlistId, SONG_3, null);

        JsonNode songs = getPlaylistSongs(playlistId);
        assertThat(songs).hasSize(3);
        assertThat(songs.get(0).get("position").asInt()).isEqualTo(1);
        assertThat(songs.get(1).get("position").asInt()).isEqualTo(2);
        assertThat(songs.get(2).get("position").asInt()).isEqualTo(3);
    }

    @Test
    @DisplayName("reorder updates positions")
    void reorderUpdatesPositions() throws Exception {
        Long playlistId = createPlaylist(USER_A, "Mix B", true);
        addSong(USER_A, playlistId, SONG_1, null);
        addSong(USER_A, playlistId, SONG_2, null);
        addSong(USER_A, playlistId, SONG_3, null);

        String reorderBody = """
                {
                  "songs": [
                    {"songId": %d, "position": 3},
                    {"songId": %d, "position": 1},
                    {"songId": %d, "position": 2}
                  ]
                }
                """.formatted(SONG_1, SONG_2, SONG_3);
        mockMvc.perform(put(BASE + "/" + playlistId + "/songs/reorder")
                .with(authentication(auth(USER_A, UserRole.LISTENER)))
                .contentType("application/json")
                .content(reorderBody))
                .andExpect(status().isOk());

        JsonNode songs = getPlaylistSongs(playlistId);
        assertThat(songs).hasSize(3);
        assertThat(songs.get(0).get("songId").asLong()).isEqualTo(SONG_2);
        assertThat(songs.get(1).get("songId").asLong()).isEqualTo(SONG_3);
        assertThat(songs.get(2).get("songId").asLong()).isEqualTo(SONG_1);
    }

    @Test
    @DisplayName("add duplicate song returns conflict")
    void addDuplicateSongConflict() throws Exception {
        Long playlistId = createPlaylist(USER_A, "Mix C", true);
        addSong(USER_A, playlistId, SONG_1, null);

        mockMvc.perform(post(BASE + "/" + playlistId + "/songs")
                .with(authentication(auth(USER_A, UserRole.LISTENER)))
                .contentType("application/json")
                .content("{\"songId\":" + SONG_1 + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("follow public playlist as different user succeeds")
    void followPublicAsDifferentUser() throws Exception {
        Long playlistId = createPlaylist(USER_A, "Public Mix", true);

        mockMvc.perform(post(BASE + "/" + playlistId + "/follow")
                .with(authentication(auth(USER_B, UserRole.LISTENER))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("follow own playlist returns conflict")
    void followOwnConflict() throws Exception {
        Long playlistId = createPlaylist(USER_A, "Own Mix", true);

        mockMvc.perform(post(BASE + "/" + playlistId + "/follow")
                .with(authentication(auth(USER_A, UserRole.LISTENER))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("follow private playlist as non owner returns forbidden")
    void followPrivateForbidden() throws Exception {
        Long playlistId = createPlaylist(USER_A, "Private Mix", false);

        mockMvc.perform(post(BASE + "/" + playlistId + "/follow")
                .with(authentication(auth(USER_B, UserRole.LISTENER))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("delete playlist then get returns not found")
    void deleteThenGetNotFound() throws Exception {
        Long playlistId = createPlaylist(USER_A, "Delete Mix", true);

        mockMvc.perform(delete(BASE + "/" + playlistId)
                .with(authentication(auth(USER_A, UserRole.LISTENER))))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/" + playlistId)
                .with(authentication(auth(USER_A, UserRole.LISTENER))))
                .andExpect(status().isNotFound());
    }

    private Long createPlaylist(Long userId, String name, boolean isPublic) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"isPublic\":" + isPublic + "}";
        MvcResult result = mockMvc.perform(post(BASE)
                .with(authentication(auth(userId, UserRole.LISTENER)))
                .contentType("application/json")
                .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return root(result).path("data").path("id").asLong();
    }

    private void addSong(Long userId, Long playlistId, Long songId, Integer position) throws Exception {
        String body = position == null
                ? "{\"songId\":" + songId + "}"
                : "{\"songId\":" + songId + ",\"position\":" + position + "}";
        mockMvc.perform(post(BASE + "/" + playlistId + "/songs")
                .with(authentication(auth(userId, UserRole.LISTENER)))
                .contentType("application/json")
                .content(body))
                .andExpect(status().isCreated());
    }

    private JsonNode getPlaylistSongs(Long playlistId) throws Exception {
        MvcResult result = mockMvc.perform(get(BASE + "/" + playlistId)
                .with(authentication(auth(USER_A, UserRole.LISTENER))))
                .andExpect(status().isOk())
                .andReturn();
        return root(result).path("data").path("songs");
    }

    private JsonNode root(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UsernamePasswordAuthenticationToken auth(Long userId, UserRole role) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(userId, "u" + userId, role),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }
}
