package com.revplay.musicplatform.systemplaylist.service.impl;

import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.DuplicateResourceException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.systemplaylist.dto.response.SystemPlaylistResponse;
import com.revplay.musicplatform.systemplaylist.entity.SystemPlaylist;
import com.revplay.musicplatform.systemplaylist.entity.SystemPlaylistSong;
import com.revplay.musicplatform.systemplaylist.repository.SystemPlaylistRepository;
import com.revplay.musicplatform.systemplaylist.repository.SystemPlaylistSongRepository;
import com.revplay.musicplatform.systemplaylist.service.SystemPlaylistService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemPlaylistServiceImpl implements SystemPlaylistService {

    private final SystemPlaylistRepository systemPlaylistRepository;
    private final SystemPlaylistSongRepository systemPlaylistSongRepository;
    private final SongRepository songRepository;

    public SystemPlaylistServiceImpl(
            SystemPlaylistRepository systemPlaylistRepository,
            SystemPlaylistSongRepository systemPlaylistSongRepository,
            SongRepository songRepository
    ) {
        this.systemPlaylistRepository = systemPlaylistRepository;
        this.systemPlaylistSongRepository = systemPlaylistSongRepository;
        this.songRepository = songRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemPlaylistResponse> getAllActivePlaylists() {
        return systemPlaylistRepository.findByIsActiveTrueAndDeletedAtIsNull()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getSongIdsBySlug(String slug) {
        SystemPlaylist playlist = systemPlaylistRepository.findBySlugAndDeletedAtIsNull(slug)
                .filter(SystemPlaylist::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("System playlist", slug));

        return systemPlaylistSongRepository.findBySystemPlaylistIdAndDeletedAtIsNullOrderByPositionAsc(playlist.getId())
                .stream()
                .map(SystemPlaylistSong::getSongId)
                .toList();
    }

    @Override
    @Transactional
    public void addSongsBySlug(String slug, List<Long> songIds) {
        if (songIds == null || songIds.isEmpty()) {
            throw new BadRequestException("songIds must not be empty");
        }
        Set<Long> uniqueSongIds = new HashSet<>(songIds);
        if (uniqueSongIds.size() != songIds.size()) {
            throw new BadRequestException("songIds contains duplicates");
        }

        SystemPlaylist playlist = systemPlaylistRepository.findBySlugAndDeletedAtIsNull(slug)
                .filter(SystemPlaylist::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("System playlist", slug));

        List<SystemPlaylistSong> existingSongs = systemPlaylistSongRepository
                .findBySystemPlaylistIdAndDeletedAtIsNullOrderByPositionAsc(playlist.getId());
        int nextPosition = existingSongs.isEmpty() ? 1 : existingSongs.get(existingSongs.size() - 1).getPosition() + 1;

        for (Long songId : songIds) {
            if (!songRepository.existsById(songId)) {
                throw new ResourceNotFoundException("Song", songId);
            }
            if (systemPlaylistSongRepository.existsBySystemPlaylistIdAndSongIdAndDeletedAtIsNull(playlist.getId(), songId)) {
                throw new DuplicateResourceException("Song already exists in system playlist: " + songId);
            }

            SystemPlaylistSong mapping = new SystemPlaylistSong();
            mapping.setSystemPlaylist(playlist);
            mapping.setSongId(songId);
            mapping.setPosition(nextPosition++);
            systemPlaylistSongRepository.save(mapping);
        }
    }

    @Override
    @Transactional
    public void softDeletePlaylist(String slug) {
        SystemPlaylist playlist = systemPlaylistRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("System playlist", slug));
        playlist.setIsActive(false);
        playlist.setDeletedAt(LocalDateTime.now());
        systemPlaylistRepository.save(playlist);
    }

    private SystemPlaylistResponse toResponse(SystemPlaylist playlist) {
        return SystemPlaylistResponse.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .slug(playlist.getSlug())
                .description(playlist.getDescription())
                .build();
    }
}
