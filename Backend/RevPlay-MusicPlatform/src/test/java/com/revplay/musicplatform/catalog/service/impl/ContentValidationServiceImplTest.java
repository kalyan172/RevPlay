package com.revplay.musicplatform.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.catalog.entity.Album;
import com.revplay.musicplatform.catalog.repository.AlbumRepository;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ContentValidationServiceImplTest {

    private static final Long ALBUM_ID = 10L;
    private static final Long ARTIST_ID = 20L;
    private static final Long OTHER_ARTIST_ID = 30L;
    private static final Long SONG_ID = 40L;
    private static final String TITLE = "Echo";

    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private ContentValidationServiceImpl service;

    @ParameterizedTest
    @MethodSource("invalidDurations")
    @DisplayName("validateSongDuration invalid values throw exception")
    void validateSongDurationInvalid(Integer duration) {
        assertThatThrownBy(() -> service.validateSongDuration(duration))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("validateSongDuration positive value passes")
    void validateSongDurationValid() {
        assertThatCode(() -> service.validateSongDuration(180)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateSongDuration above maximum throws exception")
    void validateSongDurationAboveMaximum() {
        assertThatThrownBy(() -> service.validateSongDuration(3601))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("song duration exceeds allowed limit");
    }

    @Test
    @DisplayName("validatePodcastEpisodeDuration valid value passes")
    void validatePodcastEpisodeDurationValid() {
        assertThatCode(() -> service.validatePodcastEpisodeDuration(7200)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validatePodcastEpisodeDuration above maximum throws exception")
    void validatePodcastEpisodeDurationAboveMaximum() {
        assertThatThrownBy(() -> service.validatePodcastEpisodeDuration(10801))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("podcast episode duration exceeds allowed limit");
    }

    @Test
    @DisplayName("validateAlbumBelongsToArtist with null album does nothing")
    void validateAlbumBelongsToArtistNullAlbum() {
        assertThatCode(() -> service.validateAlbumBelongsToArtist(null, ARTIST_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateAlbumBelongsToArtist with matching artist passes")
    void validateAlbumBelongsMatchingArtist() {
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album(ALBUM_ID, ARTIST_ID)));
        assertThatCode(() -> service.validateAlbumBelongsToArtist(ALBUM_ID, ARTIST_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateAlbumBelongsToArtist wrong artist throws bad request")
    void validateAlbumBelongsWrongArtist() {
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album(ALBUM_ID, OTHER_ARTIST_ID)));
        assertThatThrownBy(() -> service.validateAlbumBelongsToArtist(ALBUM_ID, ARTIST_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Album does not belong to the song artist");
    }

    @Test
    @DisplayName("validateAlbumBelongsToArtist missing album throws not found")
    void validateAlbumBelongsMissingAlbum() {
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.validateAlbumBelongsToArtist(ALBUM_ID, ARTIST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Album not found");
    }

    @Test
    @DisplayName("validateUniqueSongTitleWithinAlbum duplicate throws")
    void validateUniqueSongTitleDuplicate() {
        when(songRepository.existsByAlbumIdAndTitleIgnoreCaseAndIsActiveTrue(ALBUM_ID, TITLE)).thenReturn(true);
        assertThatThrownBy(() -> service.validateUniqueSongTitleWithinAlbum(ALBUM_ID, TITLE))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Duplicate song title exists in album");
    }

    @Test
    @DisplayName("validateUniqueSongTitleWithinAlbum unique passes")
    void validateUniqueSongTitleUnique() {
        when(songRepository.existsByAlbumIdAndTitleIgnoreCaseAndIsActiveTrue(ALBUM_ID, TITLE)).thenReturn(false);
        assertThatCode(() -> service.validateUniqueSongTitleWithinAlbum(ALBUM_ID, TITLE)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateUniqueSongTitleWithinAlbum ignores blank title")
    void validateUniqueSongTitleBlankTitle() {
        assertThatCode(() -> service.validateUniqueSongTitleWithinAlbum(ALBUM_ID, "   ")).doesNotThrowAnyException();

        verifyNoInteractions(songRepository);
    }

    @Test
    @DisplayName("validateUniqueSongTitleWithinAlbum trims title before lookup")
    void validateUniqueSongTitleTrimsTitle() {
        when(songRepository.existsByAlbumIdAndTitleIgnoreCaseAndIsActiveTrue(ALBUM_ID, TITLE)).thenReturn(false);

        assertThatCode(() -> service.validateUniqueSongTitleWithinAlbum(ALBUM_ID, "  " + TITLE + "  "))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateUniqueSongTitleWithinAlbumForUpdate same song id passes")
    void validateUniqueForUpdateSameSongId() {
        when(songRepository.existsByAlbumIdAndTitleIgnoreCaseAndIsActiveTrueAndSongIdNot(ALBUM_ID, TITLE, SONG_ID)).thenReturn(false);
        assertThatCode(() -> service.validateUniqueSongTitleWithinAlbumForUpdate(ALBUM_ID, TITLE, SONG_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateUniqueSongTitleWithinAlbumForUpdate different song id duplicate throws")
    void validateUniqueForUpdateDuplicate() {
        when(songRepository.existsByAlbumIdAndTitleIgnoreCaseAndIsActiveTrueAndSongIdNot(ALBUM_ID, TITLE, SONG_ID)).thenReturn(true);
        assertThatThrownBy(() -> service.validateUniqueSongTitleWithinAlbumForUpdate(ALBUM_ID, TITLE, SONG_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Duplicate song title exists in album");
    }

    @Test
    @DisplayName("validateUniqueSongTitleWithinAlbumForUpdate ignores null song id")
    void validateUniqueForUpdateNullSongId() {
        assertThatCode(() -> service.validateUniqueSongTitleWithinAlbumForUpdate(ALBUM_ID, TITLE, null))
                .doesNotThrowAnyException();

        verifyNoInteractions(songRepository);
    }

    private static Stream<Arguments> invalidDurations() {
        return Stream.of(
                Arguments.of((Integer) null),
                Arguments.of(0),
                Arguments.of(-1)
        );
    }

    private Album album(Long id, Long artistId) {
        Album album = new Album();
        album.setAlbumId(id);
        album.setArtistId(artistId);
        return album;
    }
}
