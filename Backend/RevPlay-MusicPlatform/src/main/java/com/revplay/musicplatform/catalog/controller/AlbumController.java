package com.revplay.musicplatform.catalog.controller;



import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import com.revplay.musicplatform.common.constants.ApiPaths;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.catalog.dto.request.AlbumCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.AlbumUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.AlbumResponse;
import com.revplay.musicplatform.catalog.service.AlbumService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Albums", description = "Album management for artists")
public class AlbumController {
    private final AlbumService service;

    public AlbumController(AlbumService service) {
        this.service = service;
    }

    @PostMapping(ApiPaths.ALBUMS)
    @Operation(summary = "Create album")
    public ResponseEntity<ApiResponse<AlbumResponse>> create(@Validated @RequestBody AlbumCreateRequest request) {
        AlbumResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(success(response, "Album created"));
    }

    @PutMapping(ApiPaths.ALBUMS + "/{albumId}")
    @Operation(summary = "Update album")
    public ResponseEntity<ApiResponse<AlbumResponse>> update(@PathVariable Long albumId,
                                                             @Validated @RequestBody AlbumUpdateRequest request) {
        AlbumResponse response = service.update(albumId, request);
        return ResponseEntity.ok(success(response, "Album updated"));
    }

    @GetMapping(ApiPaths.ALBUMS + "/{albumId}")
    @Operation(summary = "Get album")
    public ResponseEntity<ApiResponse<AlbumResponse>> get(@PathVariable Long albumId) {
        AlbumResponse response = service.get(albumId);
        return ResponseEntity.ok(success(response, "Album fetched"));
    }

    @DeleteMapping(ApiPaths.ALBUMS + "/{albumId}")
    @Operation(summary = "Delete album")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long albumId) {
        service.delete(albumId);
        return ResponseEntity.ok(success(null, "Album deleted"));
    }

    @GetMapping(ApiPaths.ARTISTS + "/{artistId}/albums")
    @Operation(summary = "List albums by artist")
    public ResponseEntity<ApiResponse<Page<AlbumResponse>>> listByArtist(@PathVariable Long artistId,
                                                                         Pageable pageable) {
        Page<AlbumResponse> response = service.listByArtist(artistId, pageable);
        return ResponseEntity.ok(success(response, "Albums fetched"));
    }

    @PutMapping(ApiPaths.ALBUMS + "/{albumId}/songs/{songId}")
    @Operation(summary = "Add song to album")
    public ResponseEntity<ApiResponse<Void>> addSong(@PathVariable Long albumId, @PathVariable Long songId) {
        service.addSongToAlbum(albumId, songId);
        return ResponseEntity.ok(success(null, "Song added to album"));
    }

    @DeleteMapping(ApiPaths.ALBUMS + "/{albumId}/songs/{songId}")
    @Operation(summary = "Remove song from album")
    public ResponseEntity<ApiResponse<Void>> removeSong(@PathVariable Long albumId, @PathVariable Long songId) {
        service.removeSongFromAlbum(albumId, songId);
        return ResponseEntity.ok(success(null, "Song removed from album"));
    }

    private <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

