package com.revplay.musicplatform.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.enums.ArtistType;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.audit.event.SongDeletedEvent;
import com.revplay.musicplatform.catalog.dto.request.SongCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.SongUpdateRequest;
import com.revplay.musicplatform.catalog.dto.request.SongVisibilityRequest;
import com.revplay.musicplatform.catalog.dto.response.SongResponse;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.enums.ContentVisibility;
import com.revplay.musicplatform.catalog.mapper.SongMapper;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.catalog.service.ContentValidationService;
import com.revplay.musicplatform.catalog.util.AccessValidator;
import com.revplay.musicplatform.catalog.util.AudioMetadataService;
import com.revplay.musicplatform.catalog.util.FileStorageService;
import com.revplay.musicplatform.catalog.util.SecurityUtil;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SongServiceImplTest {

    private static final Long USER_ID = 11L;
    private static final Long ARTIST_ID = 22L;
    private static final Long SONG_ID = 33L;
    private static final Long ALBUM_ID = 44L;
    private static final String ROLE_ARTIST = "ARTIST";
    private static final String SONG_TITLE = "Skyline";
    private static final String UPDATED_TITLE = "Updated";
    private static final String OLD_FILE_URL = "/api/v1/files/songs/old.mp3";
    private static final String NEW_FILE_NAME = "new.mp3";
    private static final String NEW_FILE_URL = "/api/v1/files/songs/" + NEW_FILE_NAME;
    private static final int DURATION = 180;
    private static final String MSG_NOT_ALLOWED = "Artist not allowed to upload songs";
    private static final String MSG_NOT_FOUND = "Song not found";
    private static final String MSG_ARTIST_NOT_FOUND = "Artist not found";
    private static final String MSG_OWNERSHIP_ERROR = "Album does not belong to the song artist";

    @Mock
    private SongRepository songRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private SongMapper mapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private AccessValidator accessValidator;
    @Mock
    private ContentValidationService contentValidationService;
    @Mock
    private AudioMetadataService audioMetadataService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MultipartFile audioFile;
    @Captor
    private ArgumentCaptor<SongDeletedEvent> eventCaptor;

    private SongServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SongServiceImpl(
                songRepository,
                artistRepository,
                mapper,
                fileStorageService,
                securityUtil,
                accessValidator,
                contentValidationService,
                audioMetadataService,
                eventPublisher);
    }

    @Test
    @DisplayName("create with artist and valid file saves song and returns response")
    void createArtistValid() {
        SongCreateRequest request = createRequest(ALBUM_ID, SONG_TITLE, DURATION);
        Song entity = song(SONG_ID, ARTIST_ID, SONG_TITLE, NEW_FILE_URL);
        SongResponse response = songResponse(SONG_ID, SONG_TITLE);

        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.MUSIC)));
        when(audioMetadataService.resolveDurationSeconds(audioFile, DURATION)).thenReturn(DURATION);
        when(fileStorageService.storeSong(audioFile)).thenReturn(NEW_FILE_NAME);
        when(mapper.toEntity(request, ARTIST_ID, NEW_FILE_URL)).thenReturn(entity);
        when(songRepository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        SongResponse actual = service.create(request, audioFile);

        verify(accessValidator).requireArtistOrAdmin(ROLE_ARTIST);
        verify(contentValidationService).validateAlbumBelongsToArtist(ALBUM_ID, ARTIST_ID);
        assertThat(actual.getSongId()).isEqualTo(SONG_ID);
    }

    @Test
    @DisplayName("create with podcast artist type throws not found")
    void createPodcastArtistDenied() {
        SongCreateRequest request = createRequest(ALBUM_ID, SONG_TITLE, DURATION);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.PODCAST)));

        assertThatThrownBy(() -> service.create(request, audioFile))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MSG_NOT_ALLOWED);
    }

    @Test
    @DisplayName("create with null file uses request duration")
    void createWithNullFileUsesRequestDuration() {
        SongCreateRequest request = createRequest(null, SONG_TITLE, DURATION);
        Song entity = song(SONG_ID, ARTIST_ID, SONG_TITLE, NEW_FILE_URL);
        SongResponse response = songResponse(SONG_ID, SONG_TITLE);

        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.MUSIC)));
        when(audioMetadataService.resolveDurationSeconds(null, DURATION)).thenReturn(DURATION);
        when(fileStorageService.storeSong(null)).thenReturn(NEW_FILE_NAME);
        when(mapper.toEntity(request, ARTIST_ID, NEW_FILE_URL)).thenReturn(entity);
        when(songRepository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        SongResponse actual = service.create(request, null);

        assertThat(actual.getSongId()).isEqualTo(SONG_ID);
    }

    @Test
    @DisplayName("update owned song returns updated response")
    void updateOwnedSong() {
        SongUpdateRequest request = new SongUpdateRequest();
        request.setAlbumId(ALBUM_ID);
        request.setTitle(UPDATED_TITLE);
        request.setDurationSeconds(DURATION);
        Song song = song(SONG_ID, ARTIST_ID, SONG_TITLE, OLD_FILE_URL);
        SongResponse response = songResponse(SONG_ID, UPDATED_TITLE);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(artistRepository.findById(ARTIST_ID))
                .thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.MUSIC)));
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(songRepository.save(song)).thenReturn(song);
        when(mapper.toResponse(song)).thenReturn(response);

        SongResponse actual = service.update(SONG_ID, request);

        verify(mapper).updateEntity(song, request);
        assertThat(actual.getTitle()).isEqualTo(UPDATED_TITLE);
    }

    @Test
    @DisplayName("get not found throws resource not found")
    void getNotFound() {
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(SONG_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MSG_NOT_FOUND);
    }

    @Test
    @DisplayName("get inactive song still returns response")
    void getInactiveSong() {
        Song song = song(SONG_ID, ARTIST_ID, SONG_TITLE, OLD_FILE_URL);
        song.setIsActive(Boolean.FALSE);
        SongResponse response = songResponse(SONG_ID, SONG_TITLE);

        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(mapper.toResponse(song)).thenReturn(response);

        SongResponse actual = service.get(SONG_ID);

        assertThat(actual.getSongId()).isEqualTo(SONG_ID);
    }

    @Test
    @DisplayName("delete owned song soft deletes and publishes event")
    void deleteOwnedSong() {
        Song song = song(SONG_ID, ARTIST_ID, SONG_TITLE, OLD_FILE_URL);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(artistRepository.findById(ARTIST_ID))
                .thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.MUSIC)));
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(songRepository.save(song)).thenReturn(song);

        service.delete(SONG_ID);

        assertThat(song.getIsActive()).isFalse();
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getSongId()).isEqualTo(SONG_ID);
    }

    @Test
    @DisplayName("listByArtist returns paged response with preserved metadata")
    void listByArtist() {
        PageRequest pageable = PageRequest.of(1, 2);
        Song song = song(SONG_ID, ARTIST_ID, SONG_TITLE, OLD_FILE_URL);
        SongResponse response = songResponse(SONG_ID, SONG_TITLE);
        Page<Song> page = new PageImpl<>(java.util.List.of(song), pageable, 5);

        when(songRepository.findByArtistIdAndIsActiveTrue(ARTIST_ID, pageable)).thenReturn(page);
        when(mapper.toResponse(song)).thenReturn(response);

        Page<SongResponse> actual = service.listByArtist(ARTIST_ID, pageable);

        assertThat(actual.getTotalElements()).isEqualTo(5);
        assertThat(actual.getNumber()).isEqualTo(1);
        assertThat(actual.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("updateVisibility with null isActive only updates visibility")
    void updateVisibilityNullActive() {
        Song song = song(SONG_ID, ARTIST_ID, SONG_TITLE, OLD_FILE_URL);
        song.setIsActive(Boolean.TRUE);
        SongVisibilityRequest request = new SongVisibilityRequest();
        request.setIsActive(null);
        request.setVisibility(ContentVisibility.UNLISTED);
        SongResponse response = songResponse(SONG_ID, SONG_TITLE);
        response.setVisibility(ContentVisibility.UNLISTED);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(artistRepository.findById(ARTIST_ID))
                .thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.MUSIC)));
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(songRepository.save(song)).thenReturn(song);
        when(mapper.toResponse(song)).thenReturn(response);

        SongResponse actual = service.updateVisibility(SONG_ID, request);

        assertThat(song.getIsActive()).isTrue();
        assertThat(actual.getVisibility()).isEqualTo(ContentVisibility.UNLISTED);
    }

    @Test
    @DisplayName("replaceAudio deletes previous file when url contains file name")
    void replaceAudioDeletesOldFile() {
        Song song = song(SONG_ID, ARTIST_ID, SONG_TITLE, OLD_FILE_URL);
        SongResponse response = songResponse(SONG_ID, SONG_TITLE);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(artistRepository.findById(ARTIST_ID))
                .thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.MUSIC)));
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(audioMetadataService.resolveDurationSeconds(audioFile, DURATION)).thenReturn(DURATION);
        when(fileStorageService.storeSong(audioFile)).thenReturn(NEW_FILE_NAME);
        when(songRepository.save(song)).thenReturn(song);
        when(mapper.toResponse(song)).thenReturn(response);

        service.replaceAudio(SONG_ID, audioFile);

        verify(fileStorageService).deleteSongFile("old.mp3");
    }

    @Test
    @DisplayName("replaceAudio with null fileUrl does not delete file")
    void replaceAudioNullFileUrlNoDelete() {
        Song song = song(SONG_ID, ARTIST_ID, SONG_TITLE, null);
        SongResponse response = songResponse(SONG_ID, SONG_TITLE);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(artistRepository.findById(ARTIST_ID))
                .thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.MUSIC)));
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(audioMetadataService.resolveDurationSeconds(audioFile, DURATION)).thenReturn(DURATION);
        when(fileStorageService.storeSong(audioFile)).thenReturn(NEW_FILE_NAME);
        when(songRepository.save(song)).thenReturn(song);
        when(mapper.toResponse(song)).thenReturn(response);

        service.replaceAudio(SONG_ID, audioFile);

        verify(fileStorageService, never()).deleteSongFile(any(String.class));
    }

    @Test
    @DisplayName("replaceAudio with malformed file url does not delete file")
    void replaceAudioMalformedUrlNoDelete() {
        Song song = song(SONG_ID, ARTIST_ID, SONG_TITLE, "/api/v1/songs/");
        SongResponse response = songResponse(SONG_ID, SONG_TITLE);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(artistRepository.findById(ARTIST_ID))
                .thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.MUSIC)));
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(audioMetadataService.resolveDurationSeconds(audioFile, DURATION)).thenReturn(DURATION);
        when(fileStorageService.storeSong(audioFile)).thenReturn(NEW_FILE_NAME);
        when(songRepository.save(song)).thenReturn(song);
        when(mapper.toResponse(song)).thenReturn(response);

        service.replaceAudio(SONG_ID, audioFile);

        verify(fileStorageService, never()).deleteSongFile(any(String.class));
    }

    @Test
    @DisplayName("non-owner artist cannot update song and gets not found")
    void updateNonOwnerArtist() {
        Song song = song(SONG_ID, ARTIST_ID, SONG_TITLE, OLD_FILE_URL);
        SongUpdateRequest request = new SongUpdateRequest();
        request.setTitle(UPDATED_TITLE);
        request.setDurationSeconds(DURATION);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(artistRepository.findById(ARTIST_ID))
                .thenReturn(Optional.of(artist(ARTIST_ID, USER_ID + 1, ArtistType.MUSIC)));
        when(securityUtil.getUserId()).thenReturn(USER_ID);

        assertThatThrownBy(() -> service.update(SONG_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MSG_ARTIST_NOT_FOUND);
    }

    @Test
    @DisplayName("create propagates album ownership validation error")
    void createAlbumValidationError() {
        SongCreateRequest request = createRequest(ALBUM_ID, SONG_TITLE, DURATION);

        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(artist(ARTIST_ID, USER_ID, ArtistType.MUSIC)));
        when(audioMetadataService.resolveDurationSeconds(audioFile, DURATION)).thenReturn(DURATION);
        doThrow(new BadRequestException(MSG_OWNERSHIP_ERROR))
                .when(contentValidationService).validateAlbumBelongsToArtist(ALBUM_ID, ARTIST_ID);

        assertThatThrownBy(() -> service.create(request, audioFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(MSG_OWNERSHIP_ERROR);
    }

    private SongCreateRequest createRequest(Long albumId, String title, Integer duration) {
        SongCreateRequest request = new SongCreateRequest();
        request.setAlbumId(albumId);
        request.setTitle(title);
        request.setDurationSeconds(duration);
        request.setVisibility(ContentVisibility.PUBLIC);
        return request;
    }

    private Song song(Long songId, Long artistId, String title, String fileUrl) {
        Song song = new Song();
        song.setSongId(songId);
        song.setArtistId(artistId);
        song.setTitle(title);
        song.setDurationSeconds(DURATION);
        song.setFileUrl(fileUrl);
        song.setVisibility(ContentVisibility.PUBLIC);
        song.setIsActive(Boolean.TRUE);
        return song;
    }

    private SongResponse songResponse(Long songId, String title) {
        SongResponse response = new SongResponse();
        response.setSongId(songId);
        response.setTitle(title);
        response.setVisibility(ContentVisibility.PUBLIC);
        return response;
    }

    private Artist artist(Long artistId, Long userId, ArtistType artistType) {
        Artist artist = new Artist();
        artist.setArtistId(artistId);
        artist.setUserId(userId);
        artist.setArtistType(artistType);
        return artist;
    }
}
