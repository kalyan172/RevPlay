package com.revplay.musicplatform.catalog.entity;



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
@Table(name = "podcasts")
public class Podcast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "podcast_id")
    private Long podcastId;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "cover_art_url")
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility")
    private ContentVisibility visibility;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    public void onCreate() {
        if (visibility == null) {
            visibility = ContentVisibility.PUBLIC;
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        createdAt = LocalDateTime.now();
    }
}

