package com.revplay.musicplatform.analytics.controller;

import com.revplay.musicplatform.analytics.dto.response.DashboardMetricsResponse;
import com.revplay.musicplatform.analytics.dto.response.TopArtistResponse;
import com.revplay.musicplatform.analytics.dto.response.TrendingContentResponse;
import com.revplay.musicplatform.analytics.dto.response.UserListeningStatsResponse;
import com.revplay.musicplatform.analytics.enums.TimePeriod;
import com.revplay.musicplatform.security.service.PlaybackRateLimiterService;
import com.revplay.musicplatform.analytics.service.PlaybackAnalyticsService;
import com.revplay.musicplatform.playback.util.PlaybackRequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Playback Analytics", description = "Platform analytics and user listening statistics")
public class PlaybackAnalyticsController {

    private final PlaybackAnalyticsService playbackAnalyticsService;
    private final PlaybackRateLimiterService playbackRateLimiterService;

    public PlaybackAnalyticsController(
            PlaybackAnalyticsService playbackAnalyticsService,
            PlaybackRateLimiterService playbackRateLimiterService
    ) {
        this.playbackAnalyticsService = playbackAnalyticsService;
        this.playbackRateLimiterService = playbackRateLimiterService;
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending songs or podcasts")
    public ResponseEntity<List<TrendingContentResponse>> trending(
            @RequestParam String type,
            @RequestParam String period,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request
    ) {
        playbackRateLimiterService.ensureWithinLimit(
                "analytics:trending:" + PlaybackRequestUtil.resolveClientKey(request),
                30,
                60,
                "Too many analytics requests. Please try again later."
        );
        return ResponseEntity.ok(playbackAnalyticsService.trending(type, TimePeriod.from(period), limit));
    }

    @GetMapping("/top-artists")
    @Operation(summary = "Get most played artists")
    public ResponseEntity<List<TopArtistResponse>> topArtists(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request
    ) {
        playbackRateLimiterService.ensureWithinLimit(
                "analytics:top-artists:" + PlaybackRequestUtil.resolveClientKey(request),
                30,
                60,
                "Too many analytics requests. Please try again later."
        );
        return ResponseEntity.ok(playbackAnalyticsService.topArtists(limit));
    }

    @GetMapping("/top-content")
    @Operation(summary = "Get top songs or podcast episodes by lifetime play count")
    public ResponseEntity<List<TrendingContentResponse>> topContent(
            @RequestParam String type,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request
    ) {
        playbackRateLimiterService.ensureWithinLimit(
                "analytics:top-content:" + PlaybackRequestUtil.resolveClientKey(request),
                30,
                60,
                "Too many analytics requests. Please try again later."
        );
        return ResponseEntity.ok(playbackAnalyticsService.topContent(type, limit));
    }

    @GetMapping("/user-stats/{userId:\\d+}")
    @Operation(summary = "Get listening statistics for a user")
    public ResponseEntity<UserListeningStatsResponse> userStats(@PathVariable Long userId) {
        return ResponseEntity.ok(playbackAnalyticsService.userStats(userId));
    }

    @GetMapping("/dashboard-metrics")
    @Operation(summary = "Get platform dashboard metrics")
    public ResponseEntity<DashboardMetricsResponse> dashboardMetrics(HttpServletRequest request) {
        playbackRateLimiterService.ensureWithinLimit(
                "analytics:dashboard-metrics:" + PlaybackRequestUtil.resolveClientKey(request),
                30,
                60,
                "Too many analytics requests. Please try again later."
        );
        return ResponseEntity.ok(playbackAnalyticsService.dashboardMetrics());
    }
}




