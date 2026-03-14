package com.revplay.musicplatform.analytics.controller;

import com.revplay.musicplatform.analytics.dto.response.UserStatisticsResponse;
import com.revplay.musicplatform.analytics.service.UserStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user-statistics")
@Tag(name = "User Statistics", description = "User statistics snapshot and refresh APIs")
public class UserStatisticsController {

    private final UserStatisticsService userStatisticsService;

    public UserStatisticsController(UserStatisticsService userStatisticsService) {
        this.userStatisticsService = userStatisticsService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user statistics")
    public ResponseEntity<UserStatisticsResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userStatisticsService.getByUserId(userId));
    }

    @PostMapping("/{userId}/refresh")
    @Operation(summary = "Recalculate and refresh user statistics")
    public ResponseEntity<UserStatisticsResponse> refresh(@PathVariable Long userId) {
        return ResponseEntity.ok(userStatisticsService.refreshAndGet(userId));
    }
}

