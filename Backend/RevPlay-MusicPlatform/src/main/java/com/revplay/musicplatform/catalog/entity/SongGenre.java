package com.revplay.musicplatform.catalog.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "song_genres")
public class SongGenre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "song_genre_id")
    private Long songGenreId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "genre_id", nullable = false)
    private Long genreId;

    @Version
    @Column(name = "version")
    private Long version;
}

