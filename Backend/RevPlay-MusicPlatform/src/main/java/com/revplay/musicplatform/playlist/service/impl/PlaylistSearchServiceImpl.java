package com.revplay.musicplatform.playlist.service.impl;

import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.playlist.dto.response.PlaylistResponse;
import com.revplay.musicplatform.playlist.entity.Playlist;
import com.revplay.musicplatform.playlist.mapper.PlaylistMapper;
import com.revplay.musicplatform.playlist.repository.PlaylistFollowRepository;
import com.revplay.musicplatform.playlist.repository.PlaylistRepository;
import com.revplay.musicplatform.playlist.repository.PlaylistSongRepository;
import com.revplay.musicplatform.playlist.service.PlaylistSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlaylistSearchServiceImpl implements PlaylistSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistSearchServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final PlaylistFollowRepository playlistFollowRepository;
    private final PlaylistMapper playlistMapper;

    public PlaylistSearchServiceImpl(
            PlaylistRepository playlistRepository,
            PlaylistSongRepository playlistSongRepository,
            PlaylistFollowRepository playlistFollowRepository,
            PlaylistMapper playlistMapper
    ) {
        this.playlistRepository = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.playlistFollowRepository = playlistFollowRepository;
        this.playlistMapper = playlistMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<PlaylistResponse> searchPublicPlaylists(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            throw new BadRequestException("keyword is required");
        }
        if (page < 0) {
            throw new BadRequestException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        LOGGER.info("Searching public playlists by keyword='{}', page={}, size={}", keyword, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Playlist> resultPage = playlistRepository.searchPublicPlaylistsByKeyword(keyword.trim(), pageable);

        List<PlaylistResponse> items = resultPage.getContent().stream()
                .map(playlist -> {
                    long songCount = playlistSongRepository.countByPlaylistId(playlist.getId());
                    long followerCount = playlistFollowRepository.countByPlaylistId(playlist.getId());
                    return playlistMapper.toResponse(playlist, songCount, followerCount);
                })
                .toList();

        return new PagedResponseDto<>(
                items,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                "createdAt",
                "DESC"
        );
    }
}

