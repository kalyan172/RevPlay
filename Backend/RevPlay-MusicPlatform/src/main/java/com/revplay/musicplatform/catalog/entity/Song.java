package com.revplay.musicplatform.catalog.entity;



import java.time.LocalDate;
import java.time.LocalDateTime;

import com.revplay.musicplatform.catalog.enums.ContentVisibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "songs")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "song_id")
    private Long songId;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Column(name = "album_id")
    private Long albumId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility")
    private ContentVisibility visibility;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    public void onCreate() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (visibility == null) {
            visibility = ContentVisibility.PUBLIC;
        }
        createdAt = LocalDateTime.now();
    }
}
