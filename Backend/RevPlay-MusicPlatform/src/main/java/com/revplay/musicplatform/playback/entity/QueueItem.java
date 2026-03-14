package com.revplay.musicplatform.playback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(
        name = "queue_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_queue_items_user_position",
                        columnNames = {"user_id", "position"}
                )
        },
        indexes = {
                @Index(name = "idx_queue_items_user_position", columnList = "user_id, position"),
                @Index(name = "idx_queue_items_song", columnList = "song_id"),
                @Index(name = "idx_queue_items_episode", columnList = "episode_id")
        }
)
@Getter
@Setter
public class QueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "queue_id")
    private Long queueId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id")
    private Long songId;

    @Column(name = "episode_id")
    private Long episodeId;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version")
    private Long version;
}
