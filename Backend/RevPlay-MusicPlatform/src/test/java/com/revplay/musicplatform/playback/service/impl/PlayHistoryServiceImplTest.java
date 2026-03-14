package com.revplay.musicplatform.playback.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.playback.dto.request.TrackPlayRequest;
import com.revplay.musicplatform.playback.dto.response.PlayHistoryResponse;
import com.revplay.musicplatform.playback.entity.PlayHistory;
import com.revplay.musicplatform.playback.exception.PlaybackNotFoundException;
import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import com.revplay.musicplatform.playback.mapper.PlayHistoryMapper;
import com.revplay.musicplatform.playback.repository.PlayHistoryRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PlayHistoryServiceImplTest {

    private static final Long USER_ID = 11L;
    private static final Long SONG_ID = 101L;
    private static final Long EPISODE_ID = 202L;
    private static final Long PLAY_ID = 500L;

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private PlayHistoryRepository playHistoryRepository;
    @Mock
    private PlayHistoryMapper playHistoryMapper;
    @Mock
    private SongRepository songRepository;

    private PlayHistoryServiceImpl playHistoryService;

    @BeforeEach
    void setUp() {
        playHistoryService = new PlayHistoryServiceImpl(jdbcTemplate, playHistoryRepository, playHistoryMapper,
                songRepository);
    }

    @Test
    @DisplayName("trackPlay song with provided fields saves exact values")
    void trackPlaySong() {
        Instant playedAt = Instant.parse("2026-01-01T00:00:00Z");
        TrackPlayRequest request = new TrackPlayRequest(USER_ID, SONG_ID, null, true, 120, playedAt);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);

        playHistoryService.trackPlay(request);

        ArgumentCaptor<PlayHistory> captor = ArgumentCaptor.forClass(PlayHistory.class);
        verify(playHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSongId()).isEqualTo(SONG_ID);
        assertThat(captor.getValue().getEpisodeId()).isNull();
        assertThat(captor.getValue().getPlayedAt()).isEqualTo(playedAt);
        assertThat(captor.getValue().getCompleted()).isTrue();
        assertThat(captor.getValue().getPlayDurationSeconds()).isEqualTo(120);
    }

    @Test
    @DisplayName("trackPlay for episode sets episode id and keeps song id null")
    void trackPlayEpisode() {
        TrackPlayRequest request = new TrackPlayRequest(USER_ID, null, EPISODE_ID, false, 30, Instant.now());
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);

        playHistoryService.trackPlay(request);

        ArgumentCaptor<PlayHistory> captor = ArgumentCaptor.forClass(PlayHistory.class);
        verify(playHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getEpisodeId()).isEqualTo(EPISODE_ID);
        assertThat(captor.getValue().getSongId()).isNull();
    }

    @Test
    @DisplayName("trackPlay defaults null playedAt completed and duration")
    void trackPlayDefaultsApplied() {
        TrackPlayRequest request = new TrackPlayRequest(USER_ID, SONG_ID, null, null, null, null);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);

        playHistoryService.trackPlay(request);

        ArgumentCaptor<PlayHistory> captor = ArgumentCaptor.forClass(PlayHistory.class);
        verify(playHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getPlayedAt()).isNotNull();
        assertThat(captor.getValue().getCompleted()).isFalse();
        assertThat(captor.getValue().getPlayDurationSeconds()).isZero();
    }

    @Test
    @DisplayName("trackPlay with null request throws required user validation")
    void trackPlayNullRequest() {
        assertThatThrownBy(() -> playHistoryService.trackPlay(null))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("userId is required");
    }

    @Test
    @DisplayName("trackPlay with null user id throws required user validation")
    void trackPlayNullUserId() {
        TrackPlayRequest request = new TrackPlayRequest(null, SONG_ID, null, false, 10, Instant.now());

        assertThatThrownBy(() -> playHistoryService.trackPlay(request))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("userId is required");
    }

    @Test
    @DisplayName("trackPlay with neither song nor episode throws playback validation")
    void trackPlayNeitherSongNorEpisode() {
        TrackPlayRequest request = new TrackPlayRequest(USER_ID, null, null, false, 10, Instant.now());
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);

        assertThatThrownBy(() -> playHistoryService.trackPlay(request))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("Exactly one of songId or episodeId must be provided");
    }

    @Test
    @DisplayName("trackPlay with both song and episode throws playback validation")
    void trackPlayBothSongAndEpisode() {
        TrackPlayRequest request = new TrackPlayRequest(USER_ID, SONG_ID, EPISODE_ID, false, 10, Instant.now());
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);

        assertThatThrownBy(() -> playHistoryService.trackPlay(request))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("Exactly one of songId or episodeId must be provided");
    }

    @Test
    @DisplayName("trackPlay with unknown user throws playback not found")
    void trackPlayUnknownUser() {
        TrackPlayRequest request = new TrackPlayRequest(USER_ID, SONG_ID, null, false, 10, Instant.now());
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(0L);

        assertThatThrownBy(() -> playHistoryService.trackPlay(request))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("User " + USER_ID + " does not exist");
    }

    @Test
    @DisplayName("getHistory returns ordered mapped results")
    void getHistoryReturnsData() {
        PlayHistory play = playHistory(USER_ID, SONG_ID, null);
        play.setPlayId(PLAY_ID);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(playHistoryRepository.findByUserIdOrderByPlayedAtDescPlayIdDesc(USER_ID)).thenReturn(List.of(play));
        when(songRepository.existsBySongIdAndIsActiveTrue(SONG_ID)).thenReturn(true);
        when(playHistoryMapper.toDto(play))
                .thenReturn(new PlayHistoryResponse(PLAY_ID, USER_ID, SONG_ID, null, play.getPlayedAt(), true, 90));

        List<PlayHistoryResponse> result = playHistoryService.getHistory(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).songId()).isEqualTo(SONG_ID);
    }

    @Test
    @DisplayName("getHistory filters inactive songs and keeps episode entries")
    void getHistoryFiltersInactiveSong() {
        PlayHistory inactiveSong = playHistory(USER_ID, SONG_ID, null);
        PlayHistory episodePlay = playHistory(USER_ID, null, EPISODE_ID);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(playHistoryRepository.findByUserIdOrderByPlayedAtDescPlayIdDesc(USER_ID))
                .thenReturn(List.of(inactiveSong, episodePlay));
        when(songRepository.existsBySongIdAndIsActiveTrue(SONG_ID)).thenReturn(false);
        when(playHistoryMapper.toDto(episodePlay)).thenReturn(
                new PlayHistoryResponse(2L, USER_ID, null, EPISODE_ID, episodePlay.getPlayedAt(), false, 10));

        List<PlayHistoryResponse> result = playHistoryService.getHistory(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).episodeId()).isEqualTo(EPISODE_ID);
    }

    @Test
    @DisplayName("getHistory unknown user throws playback not found")
    void getHistoryUnknownUser() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(0L);

        assertThatThrownBy(() -> playHistoryService.getHistory(USER_ID))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("User " + USER_ID + " does not exist");
    }

    @Test
    @DisplayName("recentlyPlayed uses fixed page request of fifty")
    void recentlyPlayedUsesPageSize50() {
        PlayHistory play = playHistory(USER_ID, SONG_ID, null);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(playHistoryRepository.findByUserIdOrderByPlayedAtDescPlayIdDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(List.of(play));
        when(songRepository.existsBySongIdAndIsActiveTrue(SONG_ID)).thenReturn(true);
        when(playHistoryMapper.toDto(play))
                .thenReturn(new PlayHistoryResponse(1L, USER_ID, SONG_ID, null, play.getPlayedAt(), false, 10));

        List<PlayHistoryResponse> result = playHistoryService.recentlyPlayed(USER_ID);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(playHistoryRepository).findByUserIdOrderByPlayedAtDescPlayIdDesc(eq(USER_ID), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("clearHistory deletes all user records and returns count")
    void clearHistoryReturnsDeletedCount() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(1L);
        when(playHistoryRepository.deleteByUserId(USER_ID)).thenReturn(3L);

        long deleted = playHistoryService.clearHistory(USER_ID);

        assertThat(deleted).isEqualTo(3L);
    }

    @Test
    @DisplayName("clearHistory unknown user throws playback not found")
    void clearHistoryUnknownUser() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(USER_ID))).thenReturn(0L);

        assertThatThrownBy(() -> playHistoryService.clearHistory(USER_ID))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("User " + USER_ID + " does not exist");
    }

    private PlayHistory playHistory(Long userId, Long songId, Long episodeId) {
        PlayHistory playHistory = new PlayHistory();
        playHistory.setUserId(userId);
        playHistory.setSongId(songId);
        playHistory.setEpisodeId(episodeId);
        playHistory.setPlayedAt(Instant.now());
        playHistory.setCompleted(Boolean.TRUE);
        playHistory.setPlayDurationSeconds(90);
        return playHistory;
    }
}
