package com.revplay.musicplatform.catalog.controller;

import com.revplay.musicplatform.catalog.dto.request.SearchRequest;
import com.revplay.musicplatform.common.constants.ApiPaths;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.catalog.dto.response.SearchResultItemResponse;
import com.revplay.musicplatform.catalog.enums.SearchContentType;
import com.revplay.musicplatform.playlist.dto.response.PlaylistResponse;
import com.revplay.musicplatform.playlist.service.PlaylistSearchService;
import com.revplay.musicplatform.security.service.DiscoveryRateLimiterService;
import com.revplay.musicplatform.catalog.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/search")
@Tag(name = "Search", description = "Search and advanced filtering APIs")
public class SearchController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;
    private final PlaylistSearchService playlistSearchService;
    private final DiscoveryRateLimiterService discoveryRateLimiterService;

    public SearchController(
            SearchService searchService,
            PlaylistSearchService playlistSearchService,
            DiscoveryRateLimiterService discoveryRateLimiterService
    ) {
        this.searchService = searchService;
        this.playlistSearchService = playlistSearchService;
        this.discoveryRateLimiterService = discoveryRateLimiterService;
    }

    @GetMapping
    @Operation(summary = "Search across songs, albums, artists, podcasts and episodes")
    public ResponseEntity<PagedResponseDto<SearchResultItemResponse>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "genreId", required = false) Long genreId,
            @RequestParam(value = "releaseDateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDateFrom,
            @RequestParam(value = "releaseDateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDateTo,
            @RequestParam(value = "artistType", required = false) String artistType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "releaseDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            HttpServletRequest httpServletRequest
    ) {
        LOGGER.info("Received search request with type={}, page={}, size={}", type, page, size);
        String clientKey = resolveClientKey(httpServletRequest);
        discoveryRateLimiterService.ensureWithinLimit(
                "search:" + clientKey,
                60,
                60,
                "Too many search requests. Please try again later."
        );
        SearchRequest request = new SearchRequest(
                query,
                SearchContentType.from(type),
                genreId,
                releaseDateFrom,
                releaseDateTo,
                artistType,
                page,
                size,
                sortBy,
                sortDir
        );
        return ResponseEntity.ok(searchService.search(request));
    }

    @GetMapping("/playlists")
    @Operation(summary = "Search public playlists by keyword")
    public ResponseEntity<PagedResponseDto<PlaylistResponse>> searchPlaylists(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LOGGER.info("Received playlist search request, page={}, size={}", page, size);
        return ResponseEntity.ok(playlistSearchService.searchPublicPlaylists(keyword, page, size));
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }
}


