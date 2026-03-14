package com.revplay.musicplatform.playlist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.audit.service.AuditLogService;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.exception.AccessDeniedException;
import com.revplay.musicplatform.exception.DuplicateResourceException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.playlist.dto.request.AddSongToPlaylistRequest;
import com.revplay.musicplatform.playlist.dto.request.CreatePlaylistRequest;
import com.revplay.musicplatform.playlist.dto.request.ReorderPlaylistSongsRequest;
import com.revplay.musicplatform.playlist.dto.request.SongPositionRequest;
import com.revplay.musicplatform.playlist.dto.request.UpdatePlaylistRequest;
import com.revplay.musicplatform.playlist.dto.response.PlaylistDetailResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistFollowResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistSongResponse;
import com.revplay.musicplatform.playlist.entity.Playlist;
import com.revplay.musicplatform.playlist.entity.PlaylistFollow;
import com.revplay.musicplatform.playlist.entity.PlaylistSong;
import com.revplay.musicplatform.playlist.mapper.PlaylistMapper;
import com.revplay.musicplatform.playlist.mapper.PlaylistSongMapper;
import com.revplay.musicplatform.playlist.repository.PlaylistFollowRepository;
import com.revplay.musicplatform.playlist.repository.PlaylistRepository;
import com.revplay.musicplatform.playlist.repository.PlaylistSongRepository;
import com.revplay.musicplatform.security.AuthContextUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PlaylistServiceImplTest {

    private static final Long OWNER_ID = 101L;
    private static final Long OTHER_ID = 202L;
    private static final Long PLAYLIST_ID = 301L;
    private static final Long SONG_ID = 401L;
    private static final Long SONG_2_ID = 402L;
    private static final Long SONG_3_ID = 403L;
    private static final String NAME = "Road Trip";
    private static final String NEW_NAME = "New Name";
    private static final String DESC = "desc";
    private static final String NEW_DESC = "new desc";
    private static final String PRIVATE_MSG = "This playlist is private";
    private static final String OWNERSHIP_MSG = "You do not own this playlist";
    private static final String CANNOT_FOLLOW_OWN = "You cannot follow your own playlist";
    private static final String CANNOT_FOLLOW_PRIVATE = "Cannot follow a private playlist";
    private static final String ALREADY_FOLLOWING = "You are already following this playlist";

    @Mock
    private PlaylistRepository playlistRepository;
    @Mock
    private PlaylistSongRepository playlistSongRepository;
    @Mock
    private PlaylistFollowRepository playlistFollowRepository;
    @Mock
    private PlaylistMapper playlistMapper;
    @Mock
    private PlaylistSongMapper playlistSongMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private AuthContextUtil authContextUtil;
    @Captor
    private ArgumentCaptor<PlaylistSong> playlistSongCaptor;

    private PlaylistServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PlaylistServiceImpl(
                playlistRepository,
                playlistSongRepository,
                playlistFollowRepository,
                playlistMapper,
                playlistSongMapper,
                auditLogService,
                authContextUtil);
    }

    @Test
    @DisplayName("createPlaylist happy path saves playlist logs audit and returns response")
    void createPlaylistHappyPath() {
        CreatePlaylistRequest request = new CreatePlaylistRequest();
        request.setName(NAME);
        Playlist entity = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        PlaylistResponse response = playlistResponse(PLAYLIST_ID, OWNER_ID, 0);
        when(authContextUtil.requireCurrentUserId()).thenReturn(OWNER_ID);
        when(playlistMapper.toEntity(request, OWNER_ID)).thenReturn(entity);
        when(playlistRepository.save(entity)).thenReturn(entity);
        when(playlistMapper.toResponse(entity, 0, 0)).thenReturn(response);

        PlaylistResponse actual = service.createPlaylist(request);

        verify(auditLogService).logInternal(AuditActionType.PLAYLIST_CREATED, OWNER_ID, AuditEntityType.PLAYLIST,
                PLAYLIST_ID,
                "Playlist '" + NAME + "' created");
        assertThat(actual.getSongCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("getPlaylistById public playlist accessible without authentication")
    void getPlaylistPublicUnauthenticated() {
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        PlaylistSong song = playlistSong(1L, PLAYLIST_ID, SONG_ID, 1);
        PlaylistDetailResponse detail = playlistDetail(PLAYLIST_ID, OWNER_ID);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(authContextUtil.getCurrentUserIdOrNull()).thenReturn(null);
        when(playlistSongRepository.findByPlaylistIdOrderByPositionAsc(PLAYLIST_ID)).thenReturn(List.of(song));
        when(playlistFollowRepository.countByPlaylistId(PLAYLIST_ID)).thenReturn(5L);
        when(playlistMapper.toDetailResponse(playlist, 1, 5)).thenReturn(detail);
        when(playlistSongMapper.toResponse(song)).thenReturn(songResponse(SONG_ID, 1));

        PlaylistDetailResponse actual = service.getPlaylistById(PLAYLIST_ID);

        assertThat(actual.getSongs()).hasSize(1);
    }

    @Test
    @DisplayName("getPlaylistById private non-owner denied")
    void getPlaylistPrivateNonOwnerDenied() {
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, false, true);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(authContextUtil.getCurrentUserIdOrNull()).thenReturn(OTHER_ID);

        assertThatThrownBy(() -> service.getPlaylistById(PLAYLIST_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(PRIVATE_MSG);
    }

    @Test
    @DisplayName("getPlaylistById inactive playlist not found")
    void getPlaylistInactiveNotFound() {
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, false);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> service.getPlaylistById(PLAYLIST_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest
    @MethodSource("updateScenarios")
    @DisplayName("updatePlaylist updates provided fields and saves")
    void updatePlaylistScenarios(String name, String desc, Boolean isPublic) {
        UpdatePlaylistRequest request = new UpdatePlaylistRequest();
        request.setName(name);
        request.setDescription(desc);
        request.setIsPublic(isPublic);

        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        PlaylistResponse response = playlistResponse(PLAYLIST_ID, OWNER_ID, 0);

        when(authContextUtil.requireCurrentUserId()).thenReturn(OWNER_ID);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(playlist)).thenReturn(playlist);
        when(playlistMapper.toResponse(eq(playlist), any(Long.class), any(Long.class))).thenReturn(response);

        service.updatePlaylist(PLAYLIST_ID, request);

        if (name != null)
            assertThat(playlist.getName()).isEqualTo(name);
        if (desc != null)
            assertThat(playlist.getDescription()).isEqualTo(desc);
        if (isPublic != null)
            assertThat(playlist.getIsPublic()).isEqualTo(isPublic);
    }

    private static Stream<Arguments> updateScenarios() {
        return Stream.of(
                Arguments.of(NEW_NAME, null, null),
                Arguments.of(null, NEW_DESC, null),
                Arguments.of(null, null, false),
                Arguments.of(NEW_NAME, NEW_DESC, false));
    }

    @Test
    @DisplayName("updatePlaylist non-owner denied")
    void updatePlaylistNonOwnerDenied() {
        UpdatePlaylistRequest request = new UpdatePlaylistRequest();
        request.setName("new");
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        when(authContextUtil.requireCurrentUserId()).thenReturn(OTHER_ID);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> service.updatePlaylist(PLAYLIST_ID, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(OWNERSHIP_MSG);
    }

    @Test
    @DisplayName("deletePlaylist owned soft deletes and audits")
    void deletePlaylistOwned() {
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        when(authContextUtil.requireCurrentUserId()).thenReturn(OWNER_ID);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(playlist)).thenReturn(playlist);

        service.deletePlaylist(PLAYLIST_ID);

        assertThat(playlist.getIsActive()).isFalse();
        verify(auditLogService).logInternal(AuditActionType.PLAYLIST_DELETED, OWNER_ID, AuditEntityType.PLAYLIST,
                PLAYLIST_ID,
                "Playlist '" + NAME + "' deleted");
    }

    @Test
    @DisplayName("getPublicPlaylists passes pageable values and returns results")
    void getPublicPlaylistsSuccess() {
        PageRequest pageable = PageRequest.of(2, 5);
        Playlist p = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        Page<Playlist> page = new PageImpl<>(List.of(p), pageable, 1);
        when(playlistRepository.findByIsPublicTrueAndIsActiveTrue(pageable)).thenReturn(page);
        when(playlistMapper.toResponse(eq(p), any(Long.class), any(Long.class))).thenReturn(new PlaylistResponse());

        PagedResponseDto<PlaylistResponse> response = service.getPublicPlaylists(2, 5);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getMyPlaylists returns authenticated user's active playlists")
    void getMyPlaylistsSuccess() {
        PageRequest pageable = PageRequest.of(0, 10);
        Playlist p = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        when(authContextUtil.requireCurrentUserId()).thenReturn(OWNER_ID);
        when(playlistRepository.findByUserIdAndIsActiveTrue(OWNER_ID, pageable)).thenReturn(new PageImpl<>(List.of(p)));
        when(playlistMapper.toResponse(eq(p), any(Long.class), any(Long.class))).thenReturn(new PlaylistResponse());

        PagedResponseDto<PlaylistResponse> response = service.getMyPlaylists(0, 10);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("addSongToPlaylist no position appends at max plus one")
    void addSongNoPosition() {
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        AddSongToPlaylistRequest request = new AddSongToPlaylistRequest();
        request.setSongId(SONG_ID);
        when(authContextUtil.requireCurrentUserId()).thenReturn(OWNER_ID);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(playlistSongRepository.existsByPlaylistIdAndSongId(PLAYLIST_ID, SONG_ID)).thenReturn(false);
        when(playlistSongRepository.findMaxPositionByPlaylistId(PLAYLIST_ID)).thenReturn(3);
        when(playlistSongRepository.save(any(PlaylistSong.class))).thenAnswer(i -> i.getArgument(0));
        when(playlistSongMapper.toResponse(any(PlaylistSong.class))).thenReturn(songResponse(SONG_ID, 4));

        PlaylistSongResponse actual = service.addSongToPlaylist(PLAYLIST_ID, request);

        verify(playlistSongRepository).save(playlistSongCaptor.capture());
        assertThat(playlistSongCaptor.getValue().getPosition()).isEqualTo(4);
        assertThat(actual.getPosition()).isEqualTo(4);
    }

    @Test
    @DisplayName("addSongToPlaylist into existing position shifts others")
    void addSongWithShift() {
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        AddSongToPlaylistRequest request = new AddSongToPlaylistRequest();
        request.setSongId(SONG_ID);
        request.setPosition(2);
        PlaylistSong existing = playlistSong(10L, PLAYLIST_ID, SONG_2_ID, 2);
        when(authContextUtil.requireCurrentUserId()).thenReturn(OWNER_ID);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(playlistSongRepository.existsByPlaylistIdAndSongId(PLAYLIST_ID, SONG_ID)).thenReturn(false);
        when(playlistSongRepository.findMaxPositionByPlaylistId(PLAYLIST_ID)).thenReturn(3);
        when(playlistSongRepository.findByPlaylistIdOrderByPositionAsc(PLAYLIST_ID)).thenReturn(List.of(existing));
        when(playlistSongRepository.save(any(PlaylistSong.class))).thenAnswer(i -> i.getArgument(0));
        when(playlistSongMapper.toResponse(any(PlaylistSong.class))).thenReturn(songResponse(SONG_ID, 2));

        service.addSongToPlaylist(PLAYLIST_ID, request);

        verify(playlistSongRepository).save(existing);
        assertThat(existing.getPosition()).isEqualTo(3);
    }

    @Test
    @DisplayName("addSongToPlaylist invalid position throws illegal argument")
    void addSongInvalidPosition() {
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        AddSongToPlaylistRequest request = new AddSongToPlaylistRequest();
        request.setSongId(SONG_ID);
        request.setPosition(0);
        when(authContextUtil.requireCurrentUserId()).thenReturn(OWNER_ID);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(playlistSongRepository.existsByPlaylistIdAndSongId(PLAYLIST_ID, SONG_ID)).thenReturn(false);
        when(playlistSongRepository.findMaxPositionByPlaylistId(PLAYLIST_ID)).thenReturn(3);

        assertThatThrownBy(() -> service.addSongToPlaylist(PLAYLIST_ID, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("addSongToPlaylist duplicate song throws duplicate resource")
    void addSongDuplicate() {
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        AddSongToPlaylistRequest request = new AddSongToPlaylistRequest();
        request.setSongId(SONG_ID);
        when(authContextUtil.requireCurrentUserId()).thenReturn(OWNER_ID);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(playlistSongRepository.existsByPlaylistIdAndSongId(PLAYLIST_ID, SONG_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.addSongToPlaylist(PLAYLIST_ID, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("removeSongFromPlaylist rebalances later positions")
    void removeSongRebalance() {
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        PlaylistSong existing = playlistSong(10L, PLAYLIST_ID, SONG_2_ID, 2);
        PlaylistSong later = playlistSong(11L, PLAYLIST_ID, SONG_3_ID, 3);
        when(authContextUtil.requireCurrentUserId()).thenReturn(OWNER_ID);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(playlistSongRepository.findByPlaylistIdAndSongId(PLAYLIST_ID, SONG_2_ID))
                .thenReturn(Optional.of(existing));
        when(playlistSongRepository.findByPlaylistIdOrderByPositionAsc(PLAYLIST_ID)).thenReturn(List.of(later));
        when(playlistSongRepository.save(any(PlaylistSong.class))).thenAnswer(i -> i.getArgument(0));

        service.removeSongFromPlaylist(PLAYLIST_ID, SONG_2_ID);

        assertThat(later.getPosition()).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("reorderInvalidScenarios")
    @DisplayName("reorderPlaylistSongs invalid requests throw expected exception")
    void reorderInvalid(ReorderPlaylistSongsRequest request, Class<? extends Exception> ex) {
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        PlaylistSong s1 = playlistSong(1L, PLAYLIST_ID, SONG_ID, 1);
        PlaylistSong s2 = playlistSong(2L, PLAYLIST_ID, SONG_2_ID, 2);

        when(authContextUtil.requireCurrentUserId()).thenReturn(OWNER_ID);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(playlistSongRepository.findByPlaylistIdOrderByPositionAsc(PLAYLIST_ID)).thenReturn(List.of(s1, s2));

        assertThatThrownBy(() -> service.reorderPlaylistSongs(PLAYLIST_ID, request))
                .isInstanceOf(ex);
    }

    private static Stream<Arguments> reorderInvalidScenarios() {
        return Stream.of(
                Arguments.of(reorderRequest(songPos(SONG_ID, 1)), IllegalArgumentException.class), // wrong count
                Arguments.of(reorderRequest(songPos(SONG_ID, 1), songPos(999L, 2)), ResourceNotFoundException.class), // unknown
                                                                                                                      // song
                Arguments.of(reorderRequest(songPos(SONG_ID, 1), songPos(SONG_ID, 2)), IllegalArgumentException.class), // duplicate
                                                                                                                        // song
                Arguments.of(reorderRequest(songPos(SONG_ID, 1), songPos(SONG_2_ID, 1)),
                        IllegalArgumentException.class), // duplicate position
                Arguments.of(reorderRequest(songPos(SONG_ID, 1), songPos(SONG_2_ID, 3)), IllegalArgumentException.class) // non-continuous
                                                                                                                         // position
        );
    }

    @Test
    @DisplayName("followPlaylist happy path saves follow and returns response")
    void followPlaylistHappyPath() {
        Playlist playlist = playlist(PLAYLIST_ID, OWNER_ID, true, true);
        PlaylistFollow saved = PlaylistFollow.builder().id(1L).playlistId(PLAYLIST_ID).followerUserId(OTHER_ID)
                .followedAt(LocalDateTime.now()).build();
        when(authContextUtil.requireCurrentUserId()).thenReturn(OTHER_ID);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(playlistFollowRepository.existsByPlaylistIdAndFollowerUserId(PLAYLIST_ID, OTHER_ID)).thenReturn(false);
        when(playlistFollowRepository.save(any(PlaylistFollow.class))).thenReturn(saved);

        PlaylistFollowResponse response = service.followPlaylist(PLAYLIST_ID);

        assertThat(response.getPlaylistId()).isEqualTo(PLAYLIST_ID);
    }

    @ParameterizedTest
    @MethodSource("followErrorScenarios")
    @DisplayName("followPlaylist throws on invalid conditions")
    void followErrors(Long userId, Playlist playlist, boolean alreadyFollowing, String expectedMsg,
            Class<? extends Exception> ex) {
        when(authContextUtil.requireCurrentUserId()).thenReturn(userId);
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        if (alreadyFollowing) {
            when(playlistFollowRepository.existsByPlaylistIdAndFollowerUserId(PLAYLIST_ID, userId)).thenReturn(true);
        }

        assertThatThrownBy(() -> service.followPlaylist(PLAYLIST_ID))
                .isInstanceOf(ex)
                .hasMessageContaining(expectedMsg);
    }

    private static Stream<Arguments> followErrorScenarios() {
        return Stream.of(
                Arguments.of(OWNER_ID, playlist(PLAYLIST_ID, OWNER_ID, true, true), false, CANNOT_FOLLOW_OWN,
                        DuplicateResourceException.class),
                Arguments.of(OTHER_ID, playlist(PLAYLIST_ID, OWNER_ID, false, true), false, CANNOT_FOLLOW_PRIVATE,
                        AccessDeniedException.class),
                Arguments.of(OTHER_ID, playlist(PLAYLIST_ID, OWNER_ID, true, true), true, ALREADY_FOLLOWING,
                        DuplicateResourceException.class));
    }

    private static Playlist playlist(Long id, Long userId, boolean isPublic, boolean isActive) {
        return Playlist.builder()
                .id(id)
                .userId(userId)
                .name(NAME)
                .description(DESC)
                .isPublic(isPublic)
                .isActive(isActive)
                .build();
    }

    private PlaylistSong playlistSong(Long id, Long playlistId, Long songId, int position) {
        return PlaylistSong.builder().id(id).playlistId(playlistId).songId(songId).position(position).build();
    }

    private PlaylistSongResponse songResponse(Long songId, int position) {
        return PlaylistSongResponse.builder().songId(songId).position(position).build();
    }

    private PlaylistResponse playlistResponse(Long id, Long userId, long songCount) {
        return PlaylistResponse.builder().id(id).userId(userId).songCount(songCount).build();
    }

    private PlaylistDetailResponse playlistDetail(Long id, Long userId) {
        return PlaylistDetailResponse.builder().id(id).userId(userId).songs(List.of()).build();
    }

    private static SongPositionRequest songPos(Long songId, int position) {
        SongPositionRequest request = new SongPositionRequest();
        request.setSongId(songId);
        request.setPosition(position);
        return request;
    }

    private static ReorderPlaylistSongsRequest reorderRequest(SongPositionRequest... items) {
        ReorderPlaylistSongsRequest request = new ReorderPlaylistSongsRequest();
        request.setSongs(List.of(items));
        return request;
    }
}
