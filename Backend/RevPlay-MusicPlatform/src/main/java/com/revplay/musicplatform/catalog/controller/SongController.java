package com.revplay.musicplatform.catalog.controller;


import com.revplay.musicplatform.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import com.revplay.musicplatform.common.constants.ApiPaths;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.catalog.dto.request.SongCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.SongUpdateRequest;
import com.revplay.musicplatform.catalog.dto.request.SongVisibilityRequest;
import com.revplay.musicplatform.catalog.dto.response.SongResponse;
import com.revplay.musicplatform.catalog.service.SongService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Songs", description = "Song upload and management")
public class SongController {
    private final SongService songService;
    private final ObjectMapper objectMapper;

    public SongController(SongService songService, ObjectMapper objectMapper) {
        this.songService = songService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(path = ApiPaths.SONGS, consumes = "multipart/form-data")
    @Operation(summary = "Create song with audio upload")
    public ResponseEntity<ApiResponse<SongResponse>> create(@RequestPart("metadata") String metadata,
                                                            @RequestPart("file") MultipartFile file) {
        SongCreateRequest request = parseSongMetadata(metadata);
        SongResponse response = songService.create(request, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(success(response, "Song created"));
    }

    @PutMapping(ApiPaths.SONGS + "/{songId}")
    @Operation(summary = "Update song")
    public ResponseEntity<ApiResponse<SongResponse>> update(@PathVariable Long songId,
                                                            @Validated @RequestBody SongUpdateRequest request) {
        SongResponse response = songService.update(songId, request);
        return ResponseEntity.ok(success(response, "Song updated"));
    }

    @GetMapping(ApiPaths.SONGS + "/{songId}")
    @Operation(summary = "Get song")
    public ResponseEntity<ApiResponse<SongResponse>> get(@PathVariable Long songId) {
        SongResponse response = songService.get(songId);
        return ResponseEntity.ok(success(response, "Song fetched"));
    }

    @DeleteMapping(ApiPaths.SONGS + "/{songId}")
    @Operation(summary = "Delete song (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long songId) {
        songService.delete(songId);
        return ResponseEntity.ok(success(null, "Song deleted"));
    }

    @GetMapping(ApiPaths.ARTISTS + "/{artistId}/songs")
    @Operation(summary = "List songs by artist")
    public ResponseEntity<ApiResponse<Page<SongResponse>>> listByArtist(@PathVariable Long artistId,
                                                                        Pageable pageable) {
        Page<SongResponse> response = songService.listByArtist(artistId, pageable);
        return ResponseEntity.ok(success(response, "Songs fetched"));
    }

    @PatchMapping(ApiPaths.SONGS + "/{songId}/visibility")
    @Operation(summary = "Update song visibility")
    public ResponseEntity<ApiResponse<SongResponse>> updateVisibility(@PathVariable Long songId,
                                                                      @Validated @RequestBody SongVisibilityRequest request) {
        SongResponse response = songService.updateVisibility(songId, request);
        return ResponseEntity.ok(success(response, "Song visibility updated"));
    }

    @PutMapping(value = ApiPaths.SONGS + "/{songId}/audio", consumes = "multipart/form-data")
    @Operation(summary = "Replace song audio file")
    public ResponseEntity<ApiResponse<SongResponse>> replaceAudio(@PathVariable Long songId,
                                                                  @RequestPart("file") MultipartFile file) {
        SongResponse response = songService.replaceAudio(songId, file);
        return ResponseEntity.ok(success(response, "Song audio replaced"));
    }

    // Genre mapping moved to SongGenreController

    private <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private SongCreateRequest parseSongMetadata(String metadata) {
        try {
            return objectMapper.readValue(metadata, SongCreateRequest.class);
        } catch (Exception exception) {
            throw new BadRequestException("Invalid song metadata JSON");
        }
    }
}

