package com.revplay.musicplatform.catalog.service.impl;

import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DiscoveryPerformanceServiceImpl implements DiscoveryPerformanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryPerformanceService.class);

    private final JdbcTemplate jdbcTemplate;

    public DiscoveryPerformanceServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureIndexes() {
        createIndexIfPossible("songs", "idx_songs_title", "title");
        createIndexIfPossible("songs", "idx_songs_release_date", "release_date");
        createIndexIfPossible("albums", "idx_albums_title", "title");
        createIndexIfPossible("albums", "idx_albums_release_date", "release_date");
        createIndexIfPossible("artists", "idx_artists_display_name", "display_name");
        createIndexIfPossible("artists", "idx_artists_artist_type", "artist_type");
        createIndexIfPossible("podcasts", "idx_podcasts_title", "title");
        createIndexIfPossible("podcasts", "idx_podcasts_release_date", "release_date");
        createIndexIfPossible("podcast_episodes", "idx_podcast_episodes_title", "title");
        createIndexIfPossible("podcast_episodes", "idx_podcast_episodes_release_date", "release_date");
        createIndexIfPossible("song_genres", "idx_song_genres_genre_song", "genre_id, song_id");
        createIndexIfPossible("play_history", "idx_play_history_song_id", "song_id");
        createIndexIfPossible("play_history", "idx_play_history_episode_id", "episode_id");
        createIndexIfPossible("play_history", "idx_play_history_user_played_at", "user_id, played_at");
    }

    private void createIndexIfPossible(String tableName, String indexName, String columns) {
        if (!tableExists(tableName)) {
            return;
        }
        if (indexExists(tableName, indexName)) {
            return;
        }
        try {
            jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + tableName + "(" + columns + ")");
            LOGGER.info("Created index {} on table {}", indexName, tableName);
        } catch (RuntimeException ex) {
            LOGGER.warn("Could not create index {} on table {}. Continuing.", indexName, tableName);
        }
    }

    private boolean tableExists(String tableName) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """,
                Long.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """,
                Long.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }
}



