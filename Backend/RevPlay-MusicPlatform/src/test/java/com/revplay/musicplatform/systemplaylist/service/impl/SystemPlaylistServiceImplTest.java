package com.revplay.musicplatform.systemplaylist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.DuplicateResourceException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.systemplaylist.dto.response.SystemPlaylistResponse;
import com.revplay.musicplatform.systemplaylist.entity.SystemPlaylist;
import com.revplay.musicplatform.systemplaylist.entity.SystemPlaylistSong;
import com.revplay.musicplatform.systemplaylist.repository.SystemPlaylistRepository;
import com.revplay.musicplatform.systemplaylist.repository.SystemPlaylistSongRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SystemPlaylistServiceImplTest {

    private static final Long PLAYLIST_ID = 10L;
    private static final String PLAYLIST_SLUG = "focus-mix";
    private static final String PLAYLIST_NAME = "Focus Mix";
    private static final String PLAYLIST_DESCRIPTION = "Coding songs";
    private static final Long SONG_ID_1 = 1L;
    private static final Long SONG_ID_2 = 2L;
    private static final Long SONG_ID_3 = 3L;
    private static final Integer POSITION_ONE = 1;
    private static final Integer POSITION_TWO = 2;
    private static final Integer POSITION_THREE = 3;
    private static final Integer POSITION_FOUR = 4;
    private static final Integer POSITION_FIVE = 5;
    private static final int SAVE_CALL_COUNT = 2;
    private static final String EMPTY_SONG_IDS_MESSAGE = "songIds must not be empty";
    private static final String DUPLICATE_SONG_IDS_MESSAGE = "songIds contains duplicates";

    @Mock
    private SystemPlaylistRepository systemPlaylistRepository;
    @Mock
    private SystemPlaylistSongRepository systemPlaylistSongRepository;
    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private SystemPlaylistServiceImpl service;

    @Test
    @DisplayName("getAllActivePlaylists maps active entities to response list")
    void getAllActivePlaylistsMapsEntities() {
        when(systemPlaylistRepository.findByIsActiveTrueAndDeletedAtIsNull()).thenReturn(List.of(playlist()));

        List<SystemPlaylistResponse> result = service.getAllActivePlaylists();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(PLAYLIST_ID);
        assertThat(result.get(0).getSlug()).isEqualTo(PLAYLIST_SLUG);
    }

    @Test
    @DisplayName("getSongIdsBySlug returns ordered song ids for active playlist")
    void getSongIdsBySlugReturnsOrderedSongIds() {
        when(systemPlaylistRepository.findBySlugAndDeletedAtIsNull(PLAYLIST_SLUG)).thenReturn(Optional.of(playlist()));
        when(systemPlaylistSongRepository.findBySystemPlaylistIdAndDeletedAtIsNullOrderByPositionAsc(PLAYLIST_ID))
                .thenReturn(List.of(mapping(SONG_ID_1, POSITION_ONE), mapping(SONG_ID_2, POSITION_TWO)));

        List<Long> songIds = service.getSongIdsBySlug(PLAYLIST_SLUG);

        assertThat(songIds).containsExactly(SONG_ID_1, SONG_ID_2);
    }

    @Test
    @DisplayName("getSongIdsBySlug throws when playlist is missing")
    void getSongIdsBySlugThrowsWhenPlaylistMissing() {
        when(systemPlaylistRepository.findBySlugAndDeletedAtIsNull(PLAYLIST_SLUG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSongIdsBySlug(PLAYLIST_SLUG))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("System playlist not found: " + PLAYLIST_SLUG);
    }

    @Test
    @DisplayName("getSongIdsBySlug throws when playlist is inactive")
    void getSongIdsBySlugThrowsWhenPlaylistInactive() {
        SystemPlaylist inactive = playlist();
        inactive.setIsActive(false);
        when(systemPlaylistRepository.findBySlugAndDeletedAtIsNull(PLAYLIST_SLUG)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.getSongIdsBySlug(PLAYLIST_SLUG))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("System playlist not found: " + PLAYLIST_SLUG);
    }

    @Test
    @DisplayName("addSongsBySlug throws when song ids are empty")
    void addSongsBySlugThrowsWhenSongIdsEmpty() {
        assertThatThrownBy(() -> service.addSongsBySlug(PLAYLIST_SLUG, List.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(EMPTY_SONG_IDS_MESSAGE);
    }

    @Test
    @DisplayName("addSongsBySlug throws when song ids contain duplicates")
    void addSongsBySlugThrowsWhenSongIdsDuplicate() {
        assertThatThrownBy(() -> service.addSongsBySlug(PLAYLIST_SLUG, List.of(SONG_ID_1, SONG_ID_1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(DUPLICATE_SONG_IDS_MESSAGE);
    }

    @Test
    @DisplayName("addSongsBySlug throws when playlist does not exist")
    void addSongsBySlugThrowsWhenPlaylistMissing() {
        when(systemPlaylistRepository.findBySlugAndDeletedAtIsNull(PLAYLIST_SLUG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addSongsBySlug(PLAYLIST_SLUG, List.of(SONG_ID_1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("System playlist not found: " + PLAYLIST_SLUG);
    }

    @Test
    @DisplayName("addSongsBySlug throws when song does not exist")
    void addSongsBySlugThrowsWhenSongMissing() {
        when(systemPlaylistRepository.findBySlugAndDeletedAtIsNull(PLAYLIST_SLUG)).thenReturn(Optional.of(playlist()));
        when(systemPlaylistSongRepository.findBySystemPlaylistIdAndDeletedAtIsNullOrderByPositionAsc(PLAYLIST_ID))
                .thenReturn(List.of());
        when(songRepository.existsById(SONG_ID_1)).thenReturn(false);

        assertThatThrownBy(() -> service.addSongsBySlug(PLAYLIST_SLUG, List.of(SONG_ID_1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Song not found: " + SONG_ID_1);
    }

    @Test
    @DisplayName("addSongsBySlug throws when song already exists in playlist")
    void addSongsBySlugThrowsWhenSongAlreadyMapped() {
        when(systemPlaylistRepository.findBySlugAndDeletedAtIsNull(PLAYLIST_SLUG)).thenReturn(Optional.of(playlist()));
        when(systemPlaylistSongRepository.findBySystemPlaylistIdAndDeletedAtIsNullOrderByPositionAsc(PLAYLIST_ID))
                .thenReturn(List.of());
        when(songRepository.existsById(SONG_ID_1)).thenReturn(true);
        when(systemPlaylistSongRepository.existsBySystemPlaylistIdAndSongIdAndDeletedAtIsNull(PLAYLIST_ID, SONG_ID_1))
                .thenReturn(true);

        assertThatThrownBy(() -> service.addSongsBySlug(PLAYLIST_SLUG, List.of(SONG_ID_1)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Song already exists in system playlist: " + SONG_ID_1);
    }

    @Test
    @DisplayName("addSongsBySlug appends songs using next position")
    void addSongsBySlugAppendsSongsUsingNextPosition() {
        when(systemPlaylistRepository.findBySlugAndDeletedAtIsNull(PLAYLIST_SLUG)).thenReturn(Optional.of(playlist()));
        when(systemPlaylistSongRepository.findBySystemPlaylistIdAndDeletedAtIsNullOrderByPositionAsc(PLAYLIST_ID))
                .thenReturn(List.of(mapping(SONG_ID_3, POSITION_THREE)));
        when(songRepository.existsById(SONG_ID_1)).thenReturn(true);
        when(songRepository.existsById(SONG_ID_2)).thenReturn(true);
        when(systemPlaylistSongRepository.existsBySystemPlaylistIdAndSongIdAndDeletedAtIsNull(PLAYLIST_ID, SONG_ID_1))
                .thenReturn(false);
        when(systemPlaylistSongRepository.existsBySystemPlaylistIdAndSongIdAndDeletedAtIsNull(PLAYLIST_ID, SONG_ID_2))
                .thenReturn(false);

        service.addSongsBySlug(PLAYLIST_SLUG, List.of(SONG_ID_1, SONG_ID_2));

        ArgumentCaptor<SystemPlaylistSong> captor = ArgumentCaptor.forClass(SystemPlaylistSong.class);
        verify(systemPlaylistSongRepository, times(SAVE_CALL_COUNT)).save(captor.capture());
        List<SystemPlaylistSong> saved = captor.getAllValues();
        assertThat(saved.get(0).getSongId()).isEqualTo(SONG_ID_1);
        assertThat(saved.get(0).getPosition()).isEqualTo(POSITION_FOUR);
        assertThat(saved.get(1).getSongId()).isEqualTo(SONG_ID_2);
        assertThat(saved.get(1).getPosition()).isEqualTo(POSITION_FIVE);
    }

    @Test
    @DisplayName("softDeletePlaylist marks playlist inactive and sets deletedAt")
    void softDeletePlaylistMarksInactive() {
        SystemPlaylist playlist = playlist();
        when(systemPlaylistRepository.findBySlugAndDeletedAtIsNull(PLAYLIST_SLUG)).thenReturn(Optional.of(playlist));

        service.softDeletePlaylist(PLAYLIST_SLUG);

        assertThat(playlist.getIsActive()).isFalse();
        assertThat(playlist.getDeletedAt()).isNotNull();
        verify(systemPlaylistRepository).save(playlist);
    }

    @Test
    @DisplayName("softDeletePlaylist throws when playlist does not exist")
    void softDeletePlaylistThrowsWhenMissing() {
        when(systemPlaylistRepository.findBySlugAndDeletedAtIsNull(PLAYLIST_SLUG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDeletePlaylist(PLAYLIST_SLUG))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("System playlist not found: " + PLAYLIST_SLUG);
        verify(systemPlaylistRepository, never()).save(any(SystemPlaylist.class));
    }

    private SystemPlaylist playlist() {
        SystemPlaylist playlist = new SystemPlaylist();
        playlist.setId(PLAYLIST_ID);
        playlist.setSlug(PLAYLIST_SLUG);
        playlist.setName(PLAYLIST_NAME);
        playlist.setDescription(PLAYLIST_DESCRIPTION);
        playlist.setIsActive(true);
        return playlist;
    }

    private SystemPlaylistSong mapping(Long songId, Integer position) {
        SystemPlaylistSong mapping = new SystemPlaylistSong();
        mapping.setSystemPlaylist(playlist());
        mapping.setSongId(songId);
        mapping.setPosition(position);
        return mapping;
    }
}
