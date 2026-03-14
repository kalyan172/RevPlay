package com.revplay.musicplatform.catalog.controller;

import com.revplay.musicplatform.catalog.dto.response.NewReleaseItemResponse;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.catalog.dto.response.PopularPodcastItemResponse;
import com.revplay.musicplatform.catalog.dto.response.SearchResultItemResponse;
import com.revplay.musicplatform.catalog.dto.response.TopArtistItemResponse;
import com.revplay.musicplatform.catalog.service.BrowseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/browse")
@Tag(name = "Browse", description = "Browse and category listing APIs")
public class BrowseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowseController.class);

    private final BrowseService browseService;

    public BrowseController(BrowseService browseService) {
        this.browseService = browseService;
    }

    @GetMapping("/new-releases")
    @Operation(summary = "Browse new releases")
    public ResponseEntity<PagedResponseDto<NewReleaseItemResponse>> newReleases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        LOGGER.info("Received browse new releases request page={}, size={}", page, size);
        return ResponseEntity.ok(browseService.newReleases(page, size, sortDir));
    }

    @GetMapping("/top-artists")
    @Operation(summary = "Browse top artists")
    public ResponseEntity<PagedResponseDto<TopArtistItemResponse>> topArtists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LOGGER.info("Received browse top artists request page={}, size={}", page, size);
        return ResponseEntity.ok(browseService.topArtists(page, size));
    }

    @GetMapping("/popular-podcasts")
    @Operation(summary = "Browse popular podcasts")
    public ResponseEntity<PagedResponseDto<PopularPodcastItemResponse>> popularPodcasts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LOGGER.info("Received browse popular podcasts request page={}, size={}", page, size);
        return ResponseEntity.ok(browseService.popularPodcasts(page, size));
    }

    @GetMapping("/songs")
    @Operation(summary = "Browse all songs")
    public ResponseEntity<PagedResponseDto<SearchResultItemResponse>> allSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "releaseDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        LOGGER.info("Received browse all songs request page={}, size={}", page, size);
        return ResponseEntity.ok(browseService.allSongs(page, size, sortBy, sortDir));
    }

    @GetMapping("/genres/{genreId}/songs")
    @Operation(summary = "Browse songs by genre")
    public ResponseEntity<PagedResponseDto<SearchResultItemResponse>> songsByGenre(
            @PathVariable Long genreId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "releaseDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        LOGGER.info("Received browse songs by genre request genreId={}, page={}, size={}", genreId, page, size);
        return ResponseEntity.ok(browseService.songsByGenre(genreId, page, size, sortBy, sortDir));
    }
}

