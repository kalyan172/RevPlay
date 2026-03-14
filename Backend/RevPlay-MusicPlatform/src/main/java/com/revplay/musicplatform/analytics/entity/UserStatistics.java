package com.revplay.musicplatform.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(
        name = "user_statistics",
        indexes = {
                @Index(name = "idx_user_statistics_user", columnList = "user_id")
        }
)
@Getter
@Setter
public class UserStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stat_id")
    private Long statId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "total_playlists", nullable = false)
    private Long totalPlaylists;

    @Column(name = "total_favorite_songs", nullable = false)
    private Long totalFavoriteSongs;

    @Column(name = "total_listening_time_seconds", nullable = false)
    private Long totalListeningTimeSeconds;

    @Column(name = "total_songs_played", nullable = false)
    private Long totalSongsPlayed;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Version
    @Column(name = "version")
    private Long version;
}
