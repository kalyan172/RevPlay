package com.revplay.musicplatform.playlist.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(
    name = "playlist_follows",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_playlist_follower",
        columnNames = {"playlist_id", "follower_user_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "follower_user_id", nullable = false)
    private Long followerUserId;

    @CreationTimestamp
    @Column(name = "followed_at", updatable = false)
    private LocalDateTime followedAt;

    @Version
    @Column(name = "version")
    private Long version;
}
