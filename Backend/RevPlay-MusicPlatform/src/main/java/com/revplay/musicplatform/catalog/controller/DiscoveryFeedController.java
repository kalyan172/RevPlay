package com.revplay.musicplatform.catalog.controller;

import com.revplay.musicplatform.catalog.dto.response.DiscoverWeeklyResponse;
import com.revplay.musicplatform.catalog.dto.response.DiscoveryFeedResponse;
import com.revplay.musicplatform.catalog.service.DiscoveryFeedService;
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
@RequestMapping("/api/v1/discover")
@Tag(name = "Discovery Feed", description = "Discovery weekly and feed aggregation APIs")
public class DiscoveryFeedController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryFeedController.class);

    private final DiscoveryFeedService discoveryFeedService;

    public DiscoveryFeedController(DiscoveryFeedService discoveryFeedService) {
        this.discoveryFeedService = discoveryFeedService;
    }

    @GetMapping("/weekly/{userId}")
    @Operation(summary = "Get discover weekly recommendations for a user")
    public ResponseEntity<DiscoverWeeklyResponse> discoverWeekly(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        LOGGER.info("Received discover weekly request userId={}, limit={}", userId, limit);
        return ResponseEntity.ok(discoveryFeedService.discoverWeekly(userId, limit));
    }

    @GetMapping("/feed/{userId}")
    @Operation(summary = "Get aggregated discovery feed for a user")
    public ResponseEntity<DiscoveryFeedResponse> homeFeed(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int sectionLimit
    ) {
        LOGGER.info("Received discovery home feed request userId={}, sectionLimit={}", userId, sectionLimit);
        return ResponseEntity.ok(discoveryFeedService.homeFeed(userId, sectionLimit));
    }
}

