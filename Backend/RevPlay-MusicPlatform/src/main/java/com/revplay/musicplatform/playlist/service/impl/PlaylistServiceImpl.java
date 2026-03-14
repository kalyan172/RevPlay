package com.revplay.musicplatform.playlist.service.impl;

import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.audit.service.AuditLogService;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.exception.AccessDeniedException;
import com.revplay.musicplatform.exception.DuplicateResourceException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.playlist.dto.request.*;
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
import com.revplay.musicplatform.playlist.service.PlaylistService;
import com.revplay.musicplatform.security.AuthContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final PlaylistFollowRepository playlistFollowRepository;
    private final PlaylistMapper playlistMapper;
    private final PlaylistSongMapper playlistSongMapper;
    private final AuditLogService auditLogService;
    private final AuthContextUtil authContextUtil;


    @Transactional
    public PlaylistResponse createPlaylist(CreatePlaylistRequest request) {
        Long userId = authContextUtil.requireCurrentUserId();
        Playlist playlist = playlistMapper.toEntity(request, userId);
        Playlist saved = playlistRepository.save(playlist);

        auditLogService.logInternal(AuditActionType.PLAYLIST_CREATED, userId, AuditEntityType.PLAYLIST,
                saved.getId(), "Playlist '" + saved.getName() + "' created");

        log.info("Playlist created: id={}, user={}", saved.getId(), userId);
        return playlistMapper.toResponse(saved, 0, 0);
    }


    @Transactional(readOnly = true)
    public PlaylistDetailResponse getPlaylistById(Long playlistId) {
        Playlist playlist = findPlaylistOrThrow(playlistId);
        enforceReadAccess(playlist, authContextUtil.getCurrentUserIdOrNull());

        List<PlaylistSong> songs = playlistSongRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        long followerCount = playlistFollowRepository.countByPlaylistId(playlistId);

        PlaylistDetailResponse response = playlistMapper.toDetailResponse(
                playlist, songs.size(), followerCount);

        List<PlaylistSongResponse> songResponses = songs.stream()
                .map(playlistSongMapper::toResponse)
                .toList();
        response.setSongs(songResponses);
        return response;
    }


    @Transactional
    public PlaylistResponse updatePlaylist(Long playlistId, UpdatePlaylistRequest request) {
        Long userId = authContextUtil.requireCurrentUserId();
        Playlist playlist = findPlaylistOrThrow(playlistId);
        enforceOwnership(playlist, userId);

        if (request.getName() != null) {
            playlist.setName(request.getName());
        }
        if (request.getDescription() != null) {
            playlist.setDescription(request.getDescription());
        }
        if (request.getIsPublic() != null) {
            playlist.setIsPublic(request.getIsPublic());
        }

        Playlist updated = playlistRepository.save(playlist);
        long songCount = playlistSongRepository.countByPlaylistId(playlistId);
        long followerCount = playlistFollowRepository.countByPlaylistId(playlistId);
        return playlistMapper.toResponse(updated, songCount, followerCount);
    }


    @Transactional
    public void deletePlaylist(Long playlistId) {
        Long userId = authContextUtil.requireCurrentUserId();
        Playlist playlist = findPlaylistOrThrow(playlistId);
        enforceOwnership(playlist, userId);

        playlist.setIsActive(Boolean.FALSE);
        playlistRepository.save(playlist);

        auditLogService.logInternal(AuditActionType.PLAYLIST_DELETED, userId, AuditEntityType.PLAYLIST,
                playlistId, "Playlist '" + playlist.getName() + "' deleted");

        log.info("Playlist deleted: id={}, user={}", playlistId, userId);
    }


    @Transactional(readOnly = true)
    public PagedResponseDto<PlaylistResponse> getPublicPlaylists(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Playlist> resultPage = playlistRepository.findByIsPublicTrueAndIsActiveTrue(pageable);
        Page<PlaylistResponse> responsePage = resultPage.map(p -> {
            long sc = playlistSongRepository.countByPlaylistId(p.getId());
            long fc = playlistFollowRepository.countByPlaylistId(p.getId());
            return playlistMapper.toResponse(p, sc, fc);
        });
        return PagedResponseDto.of(responsePage);
    }



    @Transactional(readOnly = true)
    public PagedResponseDto<PlaylistResponse> getMyPlaylists(int page, int size) {
        Long userId = authContextUtil.requireCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Playlist> resultPage = playlistRepository.findByUserIdAndIsActiveTrue(userId, pageable);
        Page<PlaylistResponse> responsePage = resultPage.map(p -> {
            long sc = playlistSongRepository.countByPlaylistId(p.getId());
            long fc = playlistFollowRepository.countByPlaylistId(p.getId());
            return playlistMapper.toResponse(p, sc, fc);
        });
        return PagedResponseDto.of(responsePage);
    }



    @Transactional
    public PlaylistSongResponse addSongToPlaylist(Long playlistId, AddSongToPlaylistRequest request) {
        Long userId = authContextUtil.requireCurrentUserId();
        Playlist playlist = findPlaylistOrThrow(playlistId);
        enforceOwnership(playlist, userId);

        if (playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, request.getSongId())) {
            throw new DuplicateResourceException(
                    "Song " + request.getSongId() + " is already in playlist " + playlistId);
        }

        int maxPosition = playlistSongRepository.findMaxPositionByPlaylistId(playlistId);
        int position = request.getPosition() != null ? request.getPosition() : maxPosition + 1;
        if (position < 1 || position > maxPosition + 1) {
            throw new IllegalArgumentException(
                    "Position must be between 1 and " + (maxPosition + 1));
        }

        if (position <= maxPosition) {
            shiftSongPositionsForInsert(playlistId, position);
        }

        PlaylistSong song = PlaylistSong.builder()
                .playlistId(playlistId)
                .songId(request.getSongId())
                .position(position)
                .build();

        PlaylistSong saved = playlistSongRepository.save(song);

        auditLogService.logInternal(AuditActionType.SONG_ADDED, userId, AuditEntityType.PLAYLIST,
                playlistId, "Song " + request.getSongId() + " added at position " + position);

        return playlistSongMapper.toResponse(saved);
    }


    @Transactional
    public void removeSongFromPlaylist(Long playlistId, Long songId) {
        Long userId = authContextUtil.requireCurrentUserId();
        Playlist playlist = findPlaylistOrThrow(playlistId);
        enforceOwnership(playlist, userId);

        PlaylistSong existing = playlistSongRepository.findByPlaylistIdAndSongId(playlistId, songId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Song " + songId + " not found in playlist " + playlistId));

        playlistSongRepository.deleteByPlaylistIdAndSongId(playlistId, songId);
        rebalanceSongPositionsAfterRemoval(playlistId, existing.getPosition());

        auditLogService.logInternal(AuditActionType.SONG_REMOVED, userId, AuditEntityType.PLAYLIST,
                playlistId, "Song " + songId + " removed from playlist " + playlistId);

        log.info("Song {} removed from playlist {} by user {}", songId, playlistId, userId);
    }

    @Transactional
    public List<PlaylistSongResponse> reorderPlaylistSongs(Long playlistId,
            ReorderPlaylistSongsRequest request) {
        Long userId = authContextUtil.requireCurrentUserId();
        Playlist playlist = findPlaylistOrThrow(playlistId);
        enforceOwnership(playlist, userId);

        validateReorderRequest(playlistId, request);
        for (SongPositionRequest entry : request.getSongs()) {
            PlaylistSong ps = playlistSongRepository
                    .findByPlaylistIdAndSongId(playlistId, entry.getSongId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Song " + entry.getSongId() + " not found in playlist " + playlistId));
            ps.setPosition(entry.getPosition());
            playlistSongRepository.save(ps);
        }

        return playlistSongRepository.findByPlaylistIdOrderByPositionAsc(playlistId)
                .stream()
                .map(playlistSongMapper::toResponse)
                .toList();
    }


    @Transactional
    public PlaylistFollowResponse followPlaylist(Long playlistId) {
        Long followerUserId = authContextUtil.requireCurrentUserId();
        Playlist playlist = findPlaylistOrThrow(playlistId);

        if (playlist.getUserId().equals(followerUserId)) {
            throw new DuplicateResourceException("You cannot follow your own playlist");
        }

        if (Boolean.FALSE.equals(playlist.getIsPublic())
                && !playlist.getUserId().equals(followerUserId)) {
            throw new AccessDeniedException("Cannot follow a private playlist");
        }

        if (playlistFollowRepository.existsByPlaylistIdAndFollowerUserId(playlistId, followerUserId)) {
            throw new DuplicateResourceException("You are already following this playlist");
        }

        PlaylistFollow follow = PlaylistFollow.builder()
                .playlistId(playlistId)
                .followerUserId(followerUserId)
                .build();

        PlaylistFollow saved = playlistFollowRepository.save(follow);

        auditLogService.logInternal(AuditActionType.FOLLOW_ADDED, followerUserId, AuditEntityType.PLAYLIST,
                playlistId, "User " + followerUserId + " followed playlist " + playlistId);

        return PlaylistFollowResponse.builder()
                .id(saved.getId())
                .playlistId(saved.getPlaylistId())
                .followerUserId(saved.getFollowerUserId())
                .followedAt(saved.getFollowedAt())
                .build();
    }


    @Transactional
    public void unfollowPlaylist(Long playlistId) {
        Long followerUserId = authContextUtil.requireCurrentUserId();
        findPlaylistOrThrow(playlistId);

        PlaylistFollow follow = playlistFollowRepository
                .findByPlaylistIdAndFollowerUserId(playlistId, followerUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You are not following playlist " + playlistId));

        playlistFollowRepository.delete(follow);

        auditLogService.logInternal(AuditActionType.FOLLOW_REMOVED, followerUserId, AuditEntityType.PLAYLIST,
                playlistId, "User " + followerUserId + " unfollowed playlist " + playlistId);

        log.info("User {} unfollowed playlist {}", followerUserId, playlistId);
    }



    private Playlist findPlaylistOrThrow(Long playlistId) {
        return playlistRepository.findById(playlistId)
                .filter(playlist -> Boolean.TRUE.equals(playlist.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", playlistId));
    }

    private void enforceOwnership(Playlist playlist, Long userId) {
        if (!playlist.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this playlist");
        }
    }

    private void enforceReadAccess(Playlist playlist, Long requestingUserId) {
        if (Boolean.FALSE.equals(playlist.getIsPublic())
                && (requestingUserId == null || !playlist.getUserId().equals(requestingUserId))) {
            throw new AccessDeniedException("This playlist is private");
        }
    }

    private void shiftSongPositionsForInsert(Long playlistId, int insertPosition) {
        List<PlaylistSong> songs = playlistSongRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        for (PlaylistSong song : songs) {
            if (song.getPosition() >= insertPosition) {
                song.setPosition(song.getPosition() + 1);
                playlistSongRepository.save(song);
            }
        }
    }

    private void rebalanceSongPositionsAfterRemoval(Long playlistId, int removedPosition) {
        List<PlaylistSong> songs = playlistSongRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        for (PlaylistSong song : songs) {
            if (song.getPosition() > removedPosition) {
                song.setPosition(song.getPosition() - 1);
                playlistSongRepository.save(song);
            }
        }
    }

    private void validateReorderRequest(Long playlistId, ReorderPlaylistSongsRequest request) {
        List<PlaylistSong> currentSongs = playlistSongRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        if (currentSongs.size() != request.getSongs().size()) {
            throw new IllegalArgumentException(
                    "Reorder request must include all playlist songs exactly once");
        }

        Set<Long> currentSongIds = new HashSet<>();
        for (PlaylistSong song : currentSongs) {
            currentSongIds.add(song.getSongId());
        }

        Set<Long> seenSongIds = new HashSet<>();
        Set<Integer> seenPositions = new HashSet<>();
        int expectedCount = currentSongs.size();

        for (SongPositionRequest entry : request.getSongs()) {
            if (!currentSongIds.contains(entry.getSongId())) {
                throw new ResourceNotFoundException(
                        "Song " + entry.getSongId() + " not found in playlist " + playlistId);
            }
            if (!seenSongIds.add(entry.getSongId())) {
                throw new IllegalArgumentException("Duplicate songId in reorder request: " + entry.getSongId());
            }
            if (!seenPositions.add(entry.getPosition())) {
                throw new IllegalArgumentException("Duplicate position in reorder request: " + entry.getPosition());
            }
        }

        for (int position = 1; position <= expectedCount; position++) {
            if (!seenPositions.contains(position)) {
                throw new IllegalArgumentException(
                        "Reorder positions must be continuous from 1 to " + expectedCount);
            }
        }
    }
}


