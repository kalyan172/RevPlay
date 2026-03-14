package com.revplay.musicplatform.analytics.controller;

import com.revplay.musicplatform.analytics.dto.response.ForYouRecommendationsResponse;
import com.revplay.musicplatform.analytics.dto.response.SongRecommendationResponse;
import com.revplay.musicplatform.analytics.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendations", description = "Recommendation endpoints based on playback signals")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/similar/{songId}")
    @Operation(summary = "Get similar songs")
    public ResponseEntity<List<SongRecommendationResponse>> similarSongs(
            @PathVariable Long songId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(recommendationService.similarSongs(songId, limit));
    }

    @GetMapping("/for-you/{userId}")
    @Operation(summary = "Get personalized recommendations for a user")
    public ResponseEntity<ForYouRecommendationsResponse> forYou(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(recommendationService.forUser(userId, limit));
    }
}



