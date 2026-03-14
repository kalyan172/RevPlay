package com.revplay.musicplatform.playlist.controller;

import com.revplay.musicplatform.playlist.dto.request.AddSongToPlaylistRequest;
import com.revplay.musicplatform.playlist.dto.request.CreatePlaylistRequest;
import com.revplay.musicplatform.playlist.dto.request.ReorderPlaylistSongsRequest;
import com.revplay.musicplatform.playlist.dto.request.UpdatePlaylistRequest;
import com.revplay.musicplatform.common.constants.ApiPaths;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.playlist.dto.response.PlaylistDetailResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistFollowResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistSongResponse;
import com.revplay.musicplatform.playlist.service.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(ApiPaths.API_V1 + "/playlists")
@RequiredArgsConstructor
@Tag(name = "Playlists", description = "Playlist CRUD, song management, and follow/unfollow operations")
public class PlaylistController {

    private final PlaylistService playlistService;



    @PostMapping
    @Operation(summary = "Create a new playlist", description = "Uses authenticated user from JWT")
    public ResponseEntity<ApiResponse<PlaylistResponse>> createPlaylist(
            @Valid @RequestBody CreatePlaylistRequest request) {

        PlaylistResponse response = playlistService.createPlaylist(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Playlist created successfully", response));
    }

    @GetMapping("/{playlistId}")
    @Operation(summary = "Get playlist details including songs", description = "Public playlists are readable by anyone. Private playlists require owner authentication.")
    public ResponseEntity<ApiResponse<PlaylistDetailResponse>> getPlaylist(@PathVariable Long playlistId) {

        PlaylistDetailResponse response = playlistService.getPlaylistById(playlistId);
        return ResponseEntity.ok(ApiResponse.success("Playlist retrieved", response));
    }

    @PutMapping("/{playlistId}")
    @Operation(summary = "Update playlist name, description, or visibility")
    public ResponseEntity<ApiResponse<PlaylistResponse>> updatePlaylist(
            @PathVariable Long playlistId,
            @Valid @RequestBody UpdatePlaylistRequest request) {

        PlaylistResponse response = playlistService.updatePlaylist(playlistId, request);
        return ResponseEntity.ok(ApiResponse.success("Playlist updated successfully", response));
    }

    @DeleteMapping("/{playlistId}")
    @Operation(summary = "Delete a playlist (triggers audit log)")
    public ResponseEntity<ApiResponse<Void>> deletePlaylist(@PathVariable Long playlistId) {

        playlistService.deletePlaylist(playlistId);
        return ResponseEntity.ok(ApiResponse.success("Playlist deleted successfully"));
    }

    @GetMapping("/public")
    @Operation(summary = "Browse all public playlists (paginated)")
    public ResponseEntity<ApiResponse<PagedResponseDto<PlaylistResponse>>> getPublicPlaylists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponseDto<PlaylistResponse> response = playlistService.getPublicPlaylists(page, size);
        return ResponseEntity.ok(ApiResponse.success("Public playlists retrieved", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my playlists (paginated)")
    public ResponseEntity<ApiResponse<PagedResponseDto<PlaylistResponse>>> getMyPlaylists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponseDto<PlaylistResponse> response = playlistService.getMyPlaylists(page, size);
        return ResponseEntity.ok(ApiResponse.success("My playlists retrieved", response));
    }



    @PostMapping("/{playlistId}/songs")
    @Operation(summary = "Add a song to a playlist")
    public ResponseEntity<ApiResponse<PlaylistSongResponse>> addSong(
            @PathVariable Long playlistId,
            @Valid @RequestBody AddSongToPlaylistRequest request) {

        PlaylistSongResponse response = playlistService.addSongToPlaylist(playlistId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Song added to playlist", response));
    }

    @DeleteMapping("/{playlistId}/songs/{songId}")
    @Operation(summary = "Remove a song from a playlist (triggers audit log)")
    public ResponseEntity<ApiResponse<Void>> removeSong(
            @PathVariable Long playlistId,
            @PathVariable Long songId) {

        playlistService.removeSongFromPlaylist(playlistId, songId);
        return ResponseEntity.ok(ApiResponse.success("Song removed from playlist"));
    }

    @PutMapping("/{playlistId}/songs/reorder")
    @Operation(summary = "Reorder songs within a playlist")
    public ResponseEntity<ApiResponse<List<PlaylistSongResponse>>> reorderSongs(
            @PathVariable Long playlistId,
            @Valid @RequestBody ReorderPlaylistSongsRequest request) {

        List<PlaylistSongResponse> response = playlistService.reorderPlaylistSongs(playlistId, request);
        return ResponseEntity.ok(ApiResponse.success("Songs reordered successfully", response));
    }


    @PostMapping("/{playlistId}/follow")
    @Operation(summary = "Follow a public playlist (triggers audit log)")
    public ResponseEntity<ApiResponse<PlaylistFollowResponse>> followPlaylist(
            @PathVariable Long playlistId) {

        PlaylistFollowResponse response = playlistService.followPlaylist(playlistId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Playlist followed successfully", response));
    }

    @DeleteMapping("/{playlistId}/unfollow")
    @Operation(summary = "Unfollow a playlist (triggers audit log)")
    public ResponseEntity<ApiResponse<Void>> unfollowPlaylist(
            @PathVariable Long playlistId) {

        playlistService.unfollowPlaylist(playlistId);
        return ResponseEntity.ok(ApiResponse.success("Playlist unfollowed successfully"));
    }
}
