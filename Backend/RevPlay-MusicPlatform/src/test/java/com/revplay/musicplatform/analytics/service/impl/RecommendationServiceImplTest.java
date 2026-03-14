package com.revplay.musicplatform.analytics.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.analytics.dto.response.ForYouRecommendationsResponse;
import com.revplay.musicplatform.analytics.dto.response.SongRecommendationResponse;
import com.revplay.musicplatform.catalog.mapper.SongMapper;
import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RecommendationServiceImplTest {

    private static final Long SONG_ID = 5L;
    private static final Long USER_ID = 7L;
    private static final int LIMIT = 10;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SongMapper songMapper;

    private RecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecommendationServiceImpl(jdbcTemplate);
    }

    @Test
    @DisplayName("similarSongs throws validation when song id null")
    void similarSongsNullSongId() {
        assertThatThrownBy(() -> service.similarSongs(null, LIMIT))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("songId is required");
    }

    @Test
    @DisplayName("similarSongs throws validation for invalid limit")
    void similarSongsInvalidLimit() {
        assertThatThrownBy(() -> service.similarSongs(SONG_ID, 0))
                .isInstanceOf(PlaybackValidationException.class);
    }

    @Test
    @DisplayName("similarSongs returns query results on success")
    @SuppressWarnings("unchecked")
    void similarSongsSuccess() {
        List<SongRecommendationResponse> rows = List.of(new SongRecommendationResponse(1L, "S", 2L, "A", 9L));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(SONG_ID), eq(LIMIT))).thenReturn(rows);

        List<SongRecommendationResponse> actual = service.similarSongs(SONG_ID, LIMIT);

        assertThat(actual).containsExactlyElementsOf(rows);
    }

    @Test
    @DisplayName("similarSongs returns empty list when query throws data access exception")
    @SuppressWarnings("unchecked")
    void similarSongsDataAccessFailure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(SONG_ID), eq(LIMIT)))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        List<SongRecommendationResponse> actual = service.similarSongs(SONG_ID, LIMIT);

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("forUser throws validation when user id null")
    void forUserNullUserId() {
        assertThatThrownBy(() -> service.forUser(null, LIMIT))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("userId is required");
    }

    @Test
    @DisplayName("forUser returns two recommendation lists on success")
    @SuppressWarnings("unchecked")
    void forUserSuccess() {
        List<SongRecommendationResponse> youMightLike = List.of(new SongRecommendationResponse(1L, "S1", 2L, "A1", 5L));
        List<SongRecommendationResponse> popular = List.of(new SongRecommendationResponse(3L, "S2", 4L, "A2", 8L));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(USER_ID), eq(USER_ID), eq(LIMIT)))
                .thenReturn(youMightLike);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(USER_ID), eq(USER_ID), eq(USER_ID), eq(LIMIT)))
                .thenReturn(popular);

        ForYouRecommendationsResponse response = service.forUser(USER_ID, LIMIT);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.youMightLike()).containsExactlyElementsOf(youMightLike);
        assertThat(response.popularWithSimilarUsers()).containsExactlyElementsOf(popular);
    }

    @Test
    @DisplayName("forUser returns empty sections when query fails")
    @SuppressWarnings("unchecked")
    void forUserDataAccessFailure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyLong(), anyLong(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        ForYouRecommendationsResponse response = service.forUser(USER_ID, LIMIT);

        assertThat(response.youMightLike()).isEmpty();
        assertThat(response.popularWithSimilarUsers()).isEmpty();
    }
}
