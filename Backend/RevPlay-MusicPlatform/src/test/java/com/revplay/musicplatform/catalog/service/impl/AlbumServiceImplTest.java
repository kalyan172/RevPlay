package com.revplay.musicplatform.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.enums.ArtistType;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.audit.event.AlbumDeletedEvent;
import com.revplay.musicplatform.catalog.dto.request.AlbumCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.AlbumUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.AlbumResponse;
import com.revplay.musicplatform.catalog.entity.Album;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.mapper.AlbumMapper;
import com.revplay.musicplatform.catalog.repository.AlbumRepository;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.catalog.util.AccessValidator;
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

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AlbumServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long ARTIST_ID = 20L;
    private static final Long ALBUM_ID = 30L;
    private static final String ROLE_ARTIST = "ARTIST";
    private static final String DEBUT_TITLE = "Debut";
    private static final String UPDATED_TITLE = "Updated";
    private static final String MSG_NOT_FOUND = "Album not found";
    private static final String MSG_ARTIST_NOT_FOUND = "Artist not found";
    private static final String MSG_CANT_DELETE = "Cannot delete album with songs";

    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private SongRepository songRepository;
    @Mock
    private AlbumMapper mapper;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private AccessValidator accessValidator;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Captor
    private ArgumentCaptor<AlbumDeletedEvent> albumDeletedEventCaptor;

    private AlbumServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AlbumServiceImpl(
                albumRepository,
                artistRepository,
                songRepository,
                mapper,
                securityUtil,
                accessValidator,
                eventPublisher);
    }

    @Test
    @DisplayName("artist create album saves and returns response")
    void createAlbumArtist() {
        AlbumCreateRequest request = new AlbumCreateRequest();
        request.setTitle(DEBUT_TITLE);
        Album album = album(ALBUM_ID, ARTIST_ID, true);
        AlbumResponse response = albumResponse(ALBUM_ID, DEBUT_TITLE);

        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(artistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(mapper.toEntity(request, ARTIST_ID)).thenReturn(album);
        when(albumRepository.save(album)).thenReturn(album);
        when(mapper.toResponse(album)).thenReturn(response);

        AlbumResponse actual = service.create(request);

        assertThat(actual.getAlbumId()).isEqualTo(ALBUM_ID);
    }

    @Test
    @DisplayName("get non-existing album throws resource not found")
    void getAlbumNotFound() {
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(ALBUM_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MSG_NOT_FOUND);
    }

    @Test
    @DisplayName("get active album returns mapped response")
    void getActiveAlbum() {
        Album album = album(ALBUM_ID, ARTIST_ID, true);
        AlbumResponse response = albumResponse(ALBUM_ID, DEBUT_TITLE);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album));
        when(mapper.toResponse(album)).thenReturn(response);

        AlbumResponse actual = service.get(ALBUM_ID);

        assertThat(actual.getAlbumId()).isEqualTo(ALBUM_ID);
    }

    @Test
    @DisplayName("get inactive album throws resource not found")
    void getInactiveAlbum() {
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album(ALBUM_ID, ARTIST_ID, false)));

        assertThatThrownBy(() -> service.get(ALBUM_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MSG_NOT_FOUND);
    }

    @Test
    @DisplayName("update owned album saves and returns response")
    void updateOwnedAlbum() {
        AlbumUpdateRequest request = new AlbumUpdateRequest();
        request.setTitle(UPDATED_TITLE);
        Album album = album(ALBUM_ID, ARTIST_ID, true);
        AlbumResponse response = albumResponse(ALBUM_ID, UPDATED_TITLE);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(albumRepository.save(album)).thenReturn(album);
        when(mapper.toResponse(album)).thenReturn(response);

        AlbumResponse actual = service.update(ALBUM_ID, request);

        verify(mapper).updateEntity(album, request);
        assertThat(actual.getTitle()).isEqualTo(UPDATED_TITLE);
    }

    @Test
    @DisplayName("update non-owned album throws resource not found")
    void updateNonOwnedAlbum() {
        AlbumUpdateRequest request = new AlbumUpdateRequest();
        request.setTitle(UPDATED_TITLE);
        Album album = album(ALBUM_ID, ARTIST_ID, true);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID + 1)));

        assertThatThrownBy(() -> service.update(ALBUM_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MSG_ARTIST_NOT_FOUND);
    }

    @Test
    @DisplayName("update inactive album throws resource not found")
    void updateInactiveAlbum() {
        AlbumUpdateRequest request = new AlbumUpdateRequest();
        request.setTitle(UPDATED_TITLE);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album(ALBUM_ID, ARTIST_ID, false)));

        assertThatThrownBy(() -> service.update(ALBUM_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MSG_NOT_FOUND);
    }

    @Test
    @DisplayName("delete owned album soft deletes and publishes event")
    void deleteOwnedAlbum() {
        Album album = album(ALBUM_ID, ARTIST_ID, true);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(songRepository.countByAlbumId(ALBUM_ID)).thenReturn(0L);
        when(albumRepository.save(album)).thenReturn(album);

        service.delete(ALBUM_ID);

        assertThat(album.getIsActive()).isFalse();
        verify(eventPublisher).publishEvent(albumDeletedEventCaptor.capture());
        assertThat(albumDeletedEventCaptor.getValue().getAlbumId()).isEqualTo(ALBUM_ID);
    }

    @Test
    @DisplayName("delete album with songs throws bad request")
    void deleteWithSongs() {
        Album album = album(ALBUM_ID, ARTIST_ID, true);

        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(songRepository.countByAlbumId(ALBUM_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(ALBUM_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(MSG_CANT_DELETE);
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("delete inactive album throws resource not found")
    void deleteInactiveAlbum() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album(ALBUM_ID, ARTIST_ID, false)));

        assertThatThrownBy(() -> service.delete(ALBUM_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MSG_NOT_FOUND);
    }

    @Test
    @DisplayName("list by artist returns paginated response")
    void listByArtist() {
        PageRequest pageable = PageRequest.of(0, 2);
        Album album = album(ALBUM_ID, ARTIST_ID, true);
        AlbumResponse response = albumResponse(ALBUM_ID, DEBUT_TITLE);
        Page<Album> page = new PageImpl<>(java.util.List.of(album), pageable, 1);

        when(albumRepository.findByArtistIdAndIsActiveTrue(ARTIST_ID, pageable)).thenReturn(page);
        when(mapper.toResponse(album)).thenReturn(response);

        Page<AlbumResponse> actual = service.listByArtist(ARTIST_ID, pageable);

        assertThat(actual.getContent()).hasSize(1);
        assertThat(actual.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("add song to album updates song album id")
    void addSongToAlbum() {
        Song song = song(ARTIST_ID, null);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album(ALBUM_ID, ARTIST_ID, true)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(songRepository.findById(40L)).thenReturn(Optional.of(song));

        service.addSongToAlbum(ALBUM_ID, 40L);

        assertThat(song.getAlbumId()).isEqualTo(ALBUM_ID);
        verify(songRepository).save(song);
    }

    @Test
    @DisplayName("add song to album throws when song missing")
    void addSongToAlbumSongMissing() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album(ALBUM_ID, ARTIST_ID, true)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(songRepository.findById(40L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addSongToAlbum(ALBUM_ID, 40L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Song not found");
    }

    @Test
    @DisplayName("add song to album throws when artist does not match")
    void addSongToAlbumArtistMismatch() {
        Song song = song(ARTIST_ID + 1, null);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album(ALBUM_ID, ARTIST_ID, true)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(songRepository.findById(40L)).thenReturn(Optional.of(song));

        assertThatThrownBy(() -> service.addSongToAlbum(ALBUM_ID, 40L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Song artist mismatch");
    }

    @Test
    @DisplayName("remove song from album clears album id")
    void removeSongFromAlbum() {
        Song song = song(ARTIST_ID, ALBUM_ID);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album(ALBUM_ID, ARTIST_ID, true)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(songRepository.findById(40L)).thenReturn(Optional.of(song));

        service.removeSongFromAlbum(ALBUM_ID, 40L);

        assertThat(song.getAlbumId()).isNull();
        verify(songRepository).save(song);
    }

    @Test
    @DisplayName("remove song from album throws when song is missing")
    void removeSongFromAlbumSongMissing() {
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album(ALBUM_ID, ARTIST_ID, true)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(songRepository.findById(40L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeSongFromAlbum(ALBUM_ID, 40L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Song not found");
    }

    @Test
    @DisplayName("remove song from album throws when song is not assigned to album")
    void removeSongFromAlbumWhenSongNotInAlbum() {
        Song song = song(ARTIST_ID, null);
        when(securityUtil.getUserRole()).thenReturn(ROLE_ARTIST);
        when(securityUtil.getUserId()).thenReturn(USER_ID);
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album(ALBUM_ID, ARTIST_ID, true)));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID)));
        when(songRepository.findById(40L)).thenReturn(Optional.of(song));

        assertThatThrownBy(() -> service.removeSongFromAlbum(ALBUM_ID, 40L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Song not in album");
    }

    @Test
    @DisplayName("admin can update album owned by another user")
    void adminCanUpdateAlbumOwnedByAnotherUser() {
        AlbumUpdateRequest request = new AlbumUpdateRequest();
        request.setTitle(UPDATED_TITLE);
        Album album = album(ALBUM_ID, ARTIST_ID, true);
        AlbumResponse response = albumResponse(ALBUM_ID, UPDATED_TITLE);

        when(securityUtil.getUserRole()).thenReturn("ADMIN");
        when(albumRepository.findById(ALBUM_ID)).thenReturn(Optional.of(album));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist(ARTIST_ID, USER_ID + 1)));
        when(albumRepository.save(album)).thenReturn(album);
        when(mapper.toResponse(album)).thenReturn(response);

        AlbumResponse actual = service.update(ALBUM_ID, request);

        assertThat(actual.getTitle()).isEqualTo(UPDATED_TITLE);
    }

    private Album album(Long id, Long artistId, boolean active) {
        Album album = new Album();
        album.setAlbumId(id);
        album.setArtistId(artistId);
        album.setTitle(DEBUT_TITLE);
        album.setIsActive(active);
        return album;
    }

    private AlbumResponse albumResponse(Long id, String title) {
        AlbumResponse response = new AlbumResponse();
        response.setAlbumId(id);
        response.setTitle(title);
        return response;
    }

    private Artist artist(Long artistId, Long userId) {
        Artist artist = new Artist();
        artist.setArtistId(artistId);
        artist.setUserId(userId);
        artist.setArtistType(ArtistType.MUSIC);
        return artist;
    }

    private Song song(Long artistId, Long albumId) {
        Song song = new Song();
        song.setArtistId(artistId);
        song.setAlbumId(albumId);
        return song;
    }
}
