package com.revplay.musicplatform.playback.controller;

import com.revplay.musicplatform.playback.dto.request.TrackPlayRequest;
import com.revplay.musicplatform.playback.dto.response.PlayHistoryResponse;
import com.revplay.musicplatform.playback.service.PlayHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Play History", description = "Play history and recently played APIs")
public class PlayHistoryController {

    private final PlayHistoryService playHistoryService;

    public PlayHistoryController(PlayHistoryService playHistoryService) {
        this.playHistoryService = playHistoryService;
    }

    @PostMapping("/play-history/track")
    @Operation(summary = "Track a play event")
    public ResponseEntity<Void> trackPlay(@Valid @RequestBody TrackPlayRequest request) {
        playHistoryService.trackPlay(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/play-history/{userId}")
    @Operation(summary = "Get full play history for user")
    public ResponseEntity<List<PlayHistoryResponse>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(playHistoryService.getHistory(userId));
    }

    @DeleteMapping("/play-history/{userId}")
    @Operation(summary = "Clear play history for user")
    public ResponseEntity<Void> clearHistory(@PathVariable Long userId) {
        playHistoryService.clearHistory(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recently-played/{userId}")
    @Operation(summary = "Get recently played (last 50) for user")
    public ResponseEntity<List<PlayHistoryResponse>> recentlyPlayed(@PathVariable Long userId) {
        return ResponseEntity.ok(playHistoryService.recentlyPlayed(userId));
    }

}




