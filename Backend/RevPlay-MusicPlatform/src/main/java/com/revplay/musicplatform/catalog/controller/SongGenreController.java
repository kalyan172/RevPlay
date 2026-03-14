package com.revplay.musicplatform.catalog.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;

import com.revplay.musicplatform.common.constants.ApiPaths;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.catalog.dto.request.SongGenresRequest;
import com.revplay.musicplatform.catalog.service.SongGenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.SONGS + "/{songId}/genres")
@Tag(name = "Song Genres", description = "Song to genre assignment")
public class SongGenreController {
    private final SongGenreService service;

    public SongGenreController(SongGenreService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Add genres to song (additive)")
    public ResponseEntity<ApiResponse<Void>> assign(@PathVariable Long songId,
                                                    @Validated @RequestBody SongGenresRequest request) {
        service.addGenres(songId, request.getGenreIds());
        return ResponseEntity.ok(success("Genres added"));
    }

    @PutMapping
    @Operation(summary = "Replace song genres")
    public ResponseEntity<ApiResponse<Void>> replace(@PathVariable Long songId,
                                                     @Validated @RequestBody SongGenresRequest request) {
        service.replaceGenres(songId, request.getGenreIds());
        return ResponseEntity.ok(success("Genres updated"));
    }

    private ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

