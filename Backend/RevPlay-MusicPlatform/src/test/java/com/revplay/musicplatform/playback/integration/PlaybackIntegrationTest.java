package com.revplay.musicplatform.playback.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.enums.ContentVisibility;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import com.revplay.musicplatform.playback.repository.PlayHistoryRepository;
import com.revplay.musicplatform.playback.repository.QueueItemRepository;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.entity.User;
import com.revplay.musicplatform.user.enums.UserRole;
import com.revplay.musicplatform.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class PlaybackIntegrationTest {

    private static Long userId;
    private static Long songAId;
    private static Long songBId;
    private static Long songCId;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final QueueItemRepository queueItemRepository;
    private final PlayHistoryRepository playHistoryRepository;
    private final CacheManager cacheManager;

    @MockBean
    private DiscoveryPerformanceService discoveryPerformanceService;

    @Autowired
    PlaybackIntegrationTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            SongRepository songRepository,
            QueueItemRepository queueItemRepository,
            PlayHistoryRepository playHistoryRepository,
            CacheManager cacheManager) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.songRepository = songRepository;
        this.queueItemRepository = queueItemRepository;
        this.playHistoryRepository = playHistoryRepository;
        this.cacheManager = cacheManager;
    }

    @BeforeEach
    void setUp() {
        queueItemRepository.deleteAll();
        playHistoryRepository.deleteAll();
        songRepository.deleteAll();
        userRepository.deleteAll();

        userId = userRepository.save(user("playback@revplay.com", "playback-user")).getUserId();
        songAId = songRepository.save(song("Song-A")).getSongId();
        songBId = songRepository.save(song("Song-B")).getSongId();
        songCId = songRepository.save(song("Song-C")).getSongId();
        clearAnalyticsCaches();
    }

    @Test
    @DisplayName("record five plays then get history ordered by most recent first")
    void recordPlaysThenGetHistory() throws Exception {
        for (int i = 0; i < 5; i++) {
            Instant playedAt = Instant.parse("2026-01-01T00:00:0" + i + "Z");
            trackPlay(songAId, playedAt, false, 50 + i);
        }

        MvcResult result = mockMvc.perform(get("/api/v1/play-history/" + userId)
                .with(authentication(auth())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = root(result).path("data");
        assertThat(data).hasSize(5);
        String firstPlayedAt = data.get(0).path("playedAt").asText();
        String lastPlayedAt = data.get(4).path("playedAt").asText();
        assertThat(firstPlayedAt.compareTo(lastPlayedAt)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("track play with completed true persists completed flag")
    void trackPlayCompletedTruePersists() throws Exception {
        trackPlay(songAId, Instant.parse("2026-01-01T00:00:00Z"), true, 99);

        var latest = playHistoryRepository.findByUserIdOrderByPlayedAtDescPlayIdDesc(userId)
                .stream()
                .max(Comparator.comparing(ph -> ph.getPlayedAt().toEpochMilli()))
                .orElseThrow();
        assertThat(latest.getCompleted()).isTrue();
        assertThat(latest.getPlayDurationSeconds()).isEqualTo(99);
    }

    @Test
    @DisplayName("add three queue items reorder currently returns conflict")
    void addQueueThenReorder() throws Exception {
        addQueueSong(songAId);
        addQueueSong(songBId);
        addQueueSong(songCId);

        MvcResult before = mockMvc.perform(get("/api/v1/queue/" + userId).with(authentication(auth())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode beforeData = root(before).path("data");
        List<Long> queueIds = new ArrayList<>();
        queueIds.add(beforeData.get(0).path("queueId").asLong());
        queueIds.add(beforeData.get(1).path("queueId").asLong());
        queueIds.add(beforeData.get(2).path("queueId").asLong());

        mockMvc.perform(put("/api/v1/queue/reorder")
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        java.util.Map.of("userId", userId, "queueIdsInOrder",
                                List.of(queueIds.get(2), queueIds.get(1), queueIds.get(0))))))
                .andExpect(status().isConflict());

        MvcResult after = mockMvc.perform(get("/api/v1/queue/" + userId).with(authentication(auth())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode afterData = root(after).path("data");
        assertThat(afterData.get(0).path("songId").asLong()).isEqualTo(songAId);
        assertThat(afterData.get(1).path("songId").asLong()).isEqualTo(songBId);
        assertThat(afterData.get(2).path("songId").asLong()).isEqualTo(songCId);
    }

    @Test
    @DisplayName("delete all queue items then queue endpoint returns empty list")
    void deleteAllQueueItemsThenGetEmpty() throws Exception {
        addQueueSong(songAId);
        addQueueSong(songBId);

        MvcResult before = mockMvc.perform(get("/api/v1/queue/" + userId).with(authentication(auth())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode beforeData = root(before).path("data");

        for (JsonNode item : beforeData) {
            mockMvc.perform(delete("/api/v1/queue/" + item.path("queueId").asLong()).with(authentication(auth())))
                    .andExpect(status().isNoContent());
        }

        MvcResult after = mockMvc.perform(get("/api/v1/queue/" + userId).with(authentication(auth())))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(root(after).path("data").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("trackPlay evicts analytics cache entries")
    void trackPlayEvictsAnalyticsCaches() throws Exception {
        Cache trendingCache = cacheManager.getCache("analytics.trending");
        assertThat(trendingCache).isNotNull();
        trendingCache.put("song:DAILY:20", List.of("cached"));
        assertThat(trendingCache.get("song:DAILY:20")).isNotNull();

        trackPlay(songAId, Instant.parse("2026-01-01T00:00:00Z"), false, 45);

        assertThat(trendingCache.get("song:DAILY:20")).isNull();
    }

    private void addQueueSong(Long songId) throws Exception {
        mockMvc.perform(post("/api/v1/queue")
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + userId + ",\"songId\":" + songId + "}"))
                .andExpect(status().isCreated());
    }

    private void trackPlay(Long songId, Instant playedAt, boolean completed, int duration) throws Exception {
        String payload = "{\"userId\":" + userId
                + ",\"songId\":" + songId
                + ",\"completed\":" + completed
                + ",\"playDurationSeconds\":" + duration
                + ",\"playedAt\":\"" + playedAt + "\"}";

        mockMvc.perform(post("/api/v1/play-history/track")
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());
    }

    private JsonNode root(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(userId, "playback-user", UserRole.LISTENER),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));
    }

    private User user(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setRole(UserRole.LISTENER);
        user.setIsActive(Boolean.TRUE);
        user.setEmailVerified(Boolean.TRUE);
        user.setCreatedAt(Instant.now().minusSeconds(60));
        user.setUpdatedAt(Instant.now().minusSeconds(30));
        return user;
    }

    private Song song(String title) {
        Song song = new Song();
        song.setArtistId(1L);
        song.setTitle(title);
        song.setDurationSeconds(180);
        song.setFileUrl(title + ".mp3");
        song.setVisibility(ContentVisibility.PUBLIC);
        song.setIsActive(Boolean.TRUE);
        song.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        return song;
    }

    private void clearAnalyticsCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }
}
