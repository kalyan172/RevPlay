package com.revplay.musicplatform.analytics.controller;

import com.revplay.musicplatform.analytics.dto.response.ArtistDashboardResponse;
import com.revplay.musicplatform.playback.dto.response.FavoritedUserResponse;
import com.revplay.musicplatform.analytics.dto.response.ListeningTrendPointResponse;
import com.revplay.musicplatform.playback.dto.response.SongPlayCountResponse;
import com.revplay.musicplatform.analytics.dto.response.SongPopularityResponse;
import com.revplay.musicplatform.analytics.dto.response.TopListenerResponse;
import com.revplay.musicplatform.analytics.enums.TrendRange;
import com.revplay.musicplatform.security.service.PlaybackRateLimiterService;
import com.revplay.musicplatform.analytics.service.ArtistAnalyticsService;
import com.revplay.musicplatform.playback.util.PlaybackRequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics/artists")
@Tag(name = "Artist Analytics", description = "Artist-level analytics endpoints")
public class ArtistAnalyticsController {

    private final ArtistAnalyticsService artistAnalyticsService;
    private final PlaybackRateLimiterService playbackRateLimiterService;

    public ArtistAnalyticsController(
            ArtistAnalyticsService artistAnalyticsService,
            PlaybackRateLimiterService playbackRateLimiterService
    ) {
        this.artistAnalyticsService = artistAnalyticsService;
        this.playbackRateLimiterService = playbackRateLimiterService;
    }

    @GetMapping("/{artistId}/dashboard")
    @Operation(summary = "Get artist dashboard metrics")
    public ResponseEntity<ArtistDashboardResponse> dashboard(@PathVariable Long artistId, HttpServletRequest request) {
        enforceRateLimit("artist-dashboard", request);
        return ResponseEntity.ok(artistAnalyticsService.dashboard(artistId));
    }

    @GetMapping("/{artistId}/songs/{songId}/plays")
    @Operation(summary = "Get play count for a specific artist song")
    public ResponseEntity<SongPlayCountResponse> songPlayCount(
            @PathVariable Long artistId,
            @PathVariable Long songId,
            HttpServletRequest request
    ) {
        enforceRateLimit("artist-song-play-count", request);
        return ResponseEntity.ok(artistAnalyticsService.songPlayCount(artistId, songId));
    }

    @GetMapping("/{artistId}/songs/popularity")
    @Operation(summary = "Get popularity metrics for all songs of an artist")
    public ResponseEntity<List<SongPopularityResponse>> songPopularity(@PathVariable Long artistId, HttpServletRequest request) {
        enforceRateLimit("artist-song-popularity", request);
        return ResponseEntity.ok(artistAnalyticsService.songPopularity(artistId));
    }

    @GetMapping("/{artistId}/favorites/users")
    @Operation(summary = "Get users who favorited artist songs")
    public ResponseEntity<List<FavoritedUserResponse>> favoritedUsers(@PathVariable Long artistId, HttpServletRequest request) {
        enforceRateLimit("artist-favorited-users", request);
        return ResponseEntity.ok(artistAnalyticsService.usersWhoFavoritedSongs(artistId));
    }

    @GetMapping("/{artistId}/trends")
    @Operation(summary = "Get listening trend points for an artist")
    public ResponseEntity<List<ListeningTrendPointResponse>> trends(
            @PathVariable Long artistId,
            @RequestParam(defaultValue = "DAILY") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request
    ) {
        enforceRateLimit("artist-trends", request);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        return ResponseEntity.ok(artistAnalyticsService.listeningTrends(
                artistId,
                TrendRange.from(range),
                start,
                end
        ));
    }

    @GetMapping("/{artistId}/top-listeners")
    @Operation(summary = "Get top listeners for an artist")
    public ResponseEntity<List<TopListenerResponse>> topListeners(
            @PathVariable Long artistId,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request
    ) {
        enforceRateLimit("artist-top-listeners", request);
        return ResponseEntity.ok(artistAnalyticsService.topListeners(artistId, limit));
    }

    private void enforceRateLimit(String endpoint, HttpServletRequest request) {
        playbackRateLimiterService.ensureWithinLimit(
                "analytics:" + endpoint + ":" + PlaybackRequestUtil.resolveClientKey(request),
                30,
                60,
                "Too many analytics requests. Please try again later."
        );
    }
}




