package com.revplay.musicplatform.playlist.service;

import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.playlist.dto.request.AddSongToPlaylistRequest;
import com.revplay.musicplatform.playlist.dto.request.CreatePlaylistRequest;
import com.revplay.musicplatform.playlist.dto.request.ReorderPlaylistSongsRequest;
import com.revplay.musicplatform.playlist.dto.request.UpdatePlaylistRequest;
import com.revplay.musicplatform.playlist.dto.response.PlaylistDetailResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistFollowResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistSongResponse;

import java.util.List;

public interface PlaylistService {

    PlaylistResponse createPlaylist(CreatePlaylistRequest request);

    PlaylistDetailResponse getPlaylistById(Long playlistId);

    PlaylistResponse updatePlaylist(Long playlistId, UpdatePlaylistRequest request);

    void deletePlaylist(Long playlistId);

    PagedResponseDto<PlaylistResponse> getPublicPlaylists(int page, int size);

    PagedResponseDto<PlaylistResponse> getMyPlaylists(int page, int size);

    PlaylistSongResponse addSongToPlaylist(Long playlistId, AddSongToPlaylistRequest request);

    void removeSongFromPlaylist(Long playlistId, Long songId);

    List<PlaylistSongResponse> reorderPlaylistSongs(Long playlistId, ReorderPlaylistSongsRequest request);

    PlaylistFollowResponse followPlaylist(Long playlistId);

    void unfollowPlaylist(Long playlistId);
}


