package com.revplay.musicplatform.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.entity.SongGenre;
import com.revplay.musicplatform.catalog.repository.GenreRepository;
import com.revplay.musicplatform.catalog.repository.SongGenreRepository;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.catalog.util.AccessValidator;
import com.revplay.musicplatform.catalog.util.SecurityUtil;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SongGenreServiceImplTest {

    private static final Long SONG_ID = 10L;
    private static final Long ARTIST_ID = 20L;
    private static final Long OWNER_USER_ID = 30L;
    private static final Long OTHER_USER_ID = 99L;
    private static final String ROLE_ARTIST = "ARTIST";
    private static final String ROLE_ADMIN = "ADMIN";

    @Mock
    private SongGenreRepository songGenreRepository;
    @Mock
    private SongRepository songRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private AccessValidator accessValidator;
    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private SongGenreServiceImpl service;

    @Test
    @DisplayName("addGenres adds only missing unique genres")
    void addGenresAddsOnlyMissingUniqueGenres() {
        Song song = song();
        SongGenre existing = new SongGenre();
        existing.setSongId(SONG_ID);
        existing.setGenreId(1L);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(OWNER_USER_ID);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(OWNER_USER_ID)));
        when(genreRepository.countByGenreIdIn(List.of(1L, 2L, 2L))).thenReturn(3L);
        when(songGenreRepository.findBySongId(SONG_ID)).thenReturn(List.of(existing));
        when(songGenreRepository.existsBySongIdAndGenreId(SONG_ID, 2L)).thenReturn(false);

        service.addGenres(SONG_ID, List.of(1L, 2L, 2L));

        verify(songGenreRepository, times(1)).save(any(SongGenre.class));
        verify(songGenreRepository, never()).existsBySongIdAndGenreId(SONG_ID, 1L);
    }

    @Test
    @DisplayName("addGenres throws not found when song missing")
    void addGenresSongMissing() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addGenres(SONG_ID, List.of(1L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Song not found");
    }

    @Test
    @DisplayName("addGenres throws not found when non admin is not owner")
    void addGenresNotOwner() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(OTHER_USER_ID);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song()));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(OWNER_USER_ID)));

        assertThatThrownBy(() -> service.addGenres(SONG_ID, List.of(1L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Song not found");
    }

    @Test
    @DisplayName("addGenres allows admin without ownership check")
    void addGenresAdminBypassOwnership() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song()));
        when(genreRepository.countByGenreIdIn(List.of(1L))).thenReturn(1L);
        when(songGenreRepository.findBySongId(SONG_ID)).thenReturn(List.of());
        when(songGenreRepository.existsBySongIdAndGenreId(SONG_ID, 1L)).thenReturn(false);

        service.addGenres(SONG_ID, List.of(1L));

        verify(artistRepository, never()).findById(any());
        verify(songGenreRepository).save(any(SongGenre.class));
    }

    @Test
    @DisplayName("addGenres throws bad request for invalid genre ids")
    void addGenresInvalidGenreIds() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song()));
        when(genreRepository.countByGenreIdIn(List.of(1L, 2L))).thenReturn(1L);

        assertThatThrownBy(() -> service.addGenres(SONG_ID, List.of(1L, 2L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid genre ids");
    }

    @Test
    @DisplayName("replaceGenres deletes existing and saves unique genres")
    void replaceGenresReplacesAll() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ADMIN);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song()));
        when(genreRepository.countByGenreIdIn(List.of(1L, 2L, 2L))).thenReturn(3L);

        service.replaceGenres(SONG_ID, List.of(1L, 2L, 2L));

        verify(songGenreRepository).deleteBySongId(SONG_ID);
        verify(songGenreRepository, times(2)).save(any(SongGenre.class));
    }

    private Song song() {
        Song song = new Song();
        song.setSongId(SONG_ID);
        song.setArtistId(ARTIST_ID);
        return song;
    }

    private Artist artist(Long userId) {
        Artist artist = new Artist();
        artist.setArtistId(ARTIST_ID);
        artist.setUserId(userId);
        return artist;
    }
}
