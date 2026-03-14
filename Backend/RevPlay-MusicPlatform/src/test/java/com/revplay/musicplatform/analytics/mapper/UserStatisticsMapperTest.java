package com.revplay.musicplatform.analytics.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.analytics.entity.UserStatistics;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class UserStatisticsMapperTest {

    private final UserStatisticsMapper mapper = new UserStatisticsMapper();

    @Test
    @DisplayName("toDto maps user statistics entity")
    void toDtoMapsFields() {
        UserStatistics stats = new UserStatistics();
        stats.setUserId(1L);
        stats.setTotalPlaylists(2L);
        stats.setTotalFavoriteSongs(3L);
        stats.setTotalListeningTimeSeconds(4L);
        stats.setTotalSongsPlayed(5L);
        stats.setLastUpdated(Instant.parse("2026-01-01T00:00:00Z"));

        var dto = mapper.toDto(stats);

        assertThat(dto.userId()).isEqualTo(1L);
        assertThat(dto.totalSongsPlayed()).isEqualTo(5L);
    }
}
