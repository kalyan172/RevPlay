package com.revplay.musicplatform.download.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.download.entity.SongDownload;
import com.revplay.musicplatform.download.repository.SongDownloadRepository;
import com.revplay.musicplatform.download.service.SongFileResolver;
import com.revplay.musicplatform.exception.AccessDeniedException;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.premium.service.SubscriptionService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class DownloadServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long SONG_ID = 20L;
    private static final String FILE_URL = "/api/v1/files/songs/track.mp3";

    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private SongRepository songRepository;
    @Mock
    private SongFileResolver songFileResolver;
    @Mock
    private SongDownloadRepository songDownloadRepository;

    @InjectMocks
    private DownloadServiceImpl service;

    @Test
    @DisplayName("downloadSong returns resource and records first download")
    void downloadSongSuccessFirstTime() {
        Song song = song();
        Resource resource = new ByteArrayResource("abc".getBytes());
        when(subscriptionService.isUserPremium(USER_ID)).thenReturn(true);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(songFileResolver.loadSongResource(FILE_URL)).thenReturn(resource);
        when(songDownloadRepository.existsByUserIdAndSongId(USER_ID, SONG_ID)).thenReturn(false);

        Resource actual = service.downloadSong(USER_ID, SONG_ID);

        assertThat(actual).isSameAs(resource);
        verify(songDownloadRepository).save(any(SongDownload.class));
    }

    @Test
    @DisplayName("downloadSong skips save when already downloaded")
    void downloadSongAlreadyDownloaded() {
        when(subscriptionService.isUserPremium(USER_ID)).thenReturn(true);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song()));
        when(songFileResolver.loadSongResource(FILE_URL)).thenReturn(new ByteArrayResource("abc".getBytes()));
        when(songDownloadRepository.existsByUserIdAndSongId(USER_ID, SONG_ID)).thenReturn(true);

        service.downloadSong(USER_ID, SONG_ID);

        verify(songDownloadRepository, never()).save(any(SongDownload.class));
    }

    @Test
    @DisplayName("downloadSong rejects non-premium user")
    void downloadSongNonPremium() {
        when(subscriptionService.isUserPremium(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.downloadSong(USER_ID, SONG_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Premium subscription required to download songs");
    }

    @Test
    @DisplayName("downloadSong throws not found when song missing")
    void downloadSongMissingSong() {
        when(subscriptionService.isUserPremium(USER_ID)).thenReturn(true);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadSong(USER_ID, SONG_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Song not found: " + SONG_ID);
    }

    @Test
    @DisplayName("downloadSong validates user and song ids")
    void downloadSongValidateIds() {
        assertThatThrownBy(() -> service.downloadSong(0L, SONG_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("userId is required");
        assertThatThrownBy(() -> service.downloadSong(USER_ID, 0L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("songId is required");
    }

    @Test
    @DisplayName("downloadSong validates null ids")
    void downloadSongValidateNullIds() {
        assertThatThrownBy(() -> service.downloadSong(null, SONG_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("userId is required");
        assertThatThrownBy(() -> service.downloadSong(USER_ID, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("songId is required");
    }

    @Test
    @DisplayName("isDownloaded validates ids and delegates")
    void isDownloadedBehavior() {
        when(songDownloadRepository.existsByUserIdAndSongId(USER_ID, SONG_ID)).thenReturn(true);

        boolean result = service.isDownloaded(USER_ID, SONG_ID);

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("isDownloaded validates invalid userId values")
    void isDownloadedValidatesUserId(Long invalidUserId) {
        assertThatThrownBy(() -> service.isDownloaded(invalidUserId, SONG_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("userId is required");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("isDownloaded validates invalid songId values")
    void isDownloadedValidatesSongId(Long invalidSongId) {
        assertThatThrownBy(() -> service.isDownloaded(USER_ID, invalidSongId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("songId is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("getDownloadFileName uses fallback when title is blank-like")
    void getDownloadFileNameFallbackForBlankLikeTitles(String title) {
        Song song = song();
        song.setTitle(title);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));

        String fileName = service.getDownloadFileName(SONG_ID);

        assertThat(fileName).isEqualTo("song-20.mp3");
    }

    @Test
    @DisplayName("getDownloadFileName sanitizes title and appends mp3")
    void getDownloadFileNameSanitize() {
        Song song = song();
        song.setTitle("A@ B# C!");
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));

        String fileName = service.getDownloadFileName(SONG_ID);

        assertThat(fileName).isEqualTo("A-B-C.mp3");
    }

    @Test
    @DisplayName("getDownloadFileName throws when song does not exist")
    void getDownloadFileNameThrowsWhenSongMissing() {
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDownloadFileName(SONG_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Song not found: " + SONG_ID);
    }

    private Song song() {
        Song song = new Song();
        song.setSongId(SONG_ID);
        song.setFileUrl(FILE_URL);
        song.setTitle("Track");
        return song;
    }
}
