package com.revplay.musicplatform.playback.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.analytics.dto.response.SongRecommendationResponse;
import com.revplay.musicplatform.analytics.service.RecommendationService;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.playback.dto.response.QueueItemResponse;
import com.revplay.musicplatform.playback.exception.PlaybackNotFoundException;
import com.revplay.musicplatform.playback.service.QueueService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AutoplayServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long CURRENT_SONG_ID = 101L;
    private static final Long NEXT_SONG_ID = 102L;
    private static final Long QUEUE_ITEM_ID = 500L;

    @Mock
    private RecommendationService recommendationService;
    @Mock
    private SongRepository songRepository;
    @Mock
    private QueueService queueService;

    private AutoplayServiceImpl autoplayService;

    @BeforeEach
    void setUp() {
        autoplayService = new AutoplayServiceImpl(queueService, recommendationService, songRepository);
    }

    @Test
    @DisplayName("getNextSong returns next song in queue if present after current")
    void getNextSongFromQueue() {
        Song nextSong = song(NEXT_SONG_ID, "Next in Queue");
        QueueItemResponse currentItem = new QueueItemResponse(QUEUE_ITEM_ID, USER_ID, CURRENT_SONG_ID, null, 1,
                Instant.now());
        QueueItemResponse nextItem = new QueueItemResponse(QUEUE_ITEM_ID + 1, USER_ID, NEXT_SONG_ID, null, 2,
                Instant.now());

        when(queueService.getQueue(USER_ID)).thenReturn(List.of(currentItem, nextItem));
        when(queueService.next(USER_ID, QUEUE_ITEM_ID)).thenReturn(nextItem);
        when(songRepository.findById(NEXT_SONG_ID)).thenReturn(Optional.of(nextSong));

        Song actual = autoplayService.getNextSong(USER_ID, CURRENT_SONG_ID);

        assertThat(actual).isEqualTo(nextSong);
    }

    @Test
    @DisplayName("getNextSong falls back to recommendations when queue is empty")
    void getNextSongFallbackWhenQueueEmpty() {
        Song recommended = song(NEXT_SONG_ID, "Recommended");
        when(queueService.getQueue(USER_ID)).thenReturn(List.of());
        when(recommendationService.similarSongs(CURRENT_SONG_ID, 1))
                .thenReturn(List.of(new SongRecommendationResponse(NEXT_SONG_ID, "Recommended", 200L, "Artist", 90L)));
        when(songRepository.findById(NEXT_SONG_ID)).thenReturn(Optional.of(recommended));

        Song actual = autoplayService.getNextSong(USER_ID, CURRENT_SONG_ID);

        assertThat(actual).isEqualTo(recommended);
    }

    @Test
    @DisplayName("getNextSong falls back to recommendations when current song not in queue")
    void getNextSongFallbackWhenSongNotInQueue() {
        Song recommended = song(NEXT_SONG_ID, "Recommended");
        QueueItemResponse otherItem = new QueueItemResponse(QUEUE_ITEM_ID, USER_ID, 999L, null, 1, Instant.now());

        when(queueService.getQueue(USER_ID)).thenReturn(List.of(otherItem));
        when(recommendationService.similarSongs(CURRENT_SONG_ID, 1))
                .thenReturn(List.of(new SongRecommendationResponse(NEXT_SONG_ID, "Recommended", 200L, "Artist", 90L)));
        when(songRepository.findById(NEXT_SONG_ID)).thenReturn(Optional.of(recommended));

        Song actual = autoplayService.getNextSong(USER_ID, CURRENT_SONG_ID);

        assertThat(actual).isEqualTo(recommended);
    }

    @Test
    @DisplayName("getNextSong throws not found when no recommendations available")
    void getNextSongNoRecommendations() {
        when(queueService.getQueue(USER_ID)).thenReturn(List.of());
        when(recommendationService.similarSongs(CURRENT_SONG_ID, 1)).thenReturn(List.of());

        assertThatThrownBy(() -> autoplayService.getNextSong(USER_ID, CURRENT_SONG_ID))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("No autoplay song available");
    }

    private Song song(Long id, String title) {
        Song song = new Song();
        song.setSongId(id);
        song.setTitle(title);
        return song;
    }
}
