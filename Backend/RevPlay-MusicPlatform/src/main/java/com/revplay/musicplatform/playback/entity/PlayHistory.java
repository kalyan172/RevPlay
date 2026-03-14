package com.revplay.musicplatform.playback.entity;

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
        name = "play_history",
        indexes = {
                @Index(name = "idx_play_history_played_at", columnList = "played_at"),
                @Index(name = "idx_play_history_user_played_at", columnList = "user_id, played_at"),
                @Index(name = "idx_play_history_user_song_played_at", columnList = "user_id, song_id, played_at"),
                @Index(name = "idx_play_history_song_played_at", columnList = "song_id, played_at"),
                @Index(name = "idx_play_history_episode_played_at", columnList = "episode_id, played_at")
        }
)
@Getter
@Setter
public class PlayHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "play_id")
    private Long playId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id")
    private Long songId;

    @Column(name = "episode_id")
    private Long episodeId;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    @Column(name = "completed")
    private Boolean completed;

    @Column(name = "play_duration_seconds")
    private Integer playDurationSeconds;

    @Version
    @Column(name = "version")
    private Long version;
}