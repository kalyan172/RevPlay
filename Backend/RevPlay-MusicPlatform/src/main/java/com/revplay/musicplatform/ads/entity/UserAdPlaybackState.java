package com.revplay.musicplatform.ads.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_ad_playback_state")
@Getter
@Setter
public class UserAdPlaybackState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "songs_played_count", nullable = false)
    private Integer songsPlayedCount;

    @Column(name = "last_served_ad_id")
    private Long lastServedAdId;
}
