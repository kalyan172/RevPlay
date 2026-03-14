package com.revplay.musicplatform.playback.controller;

import com.revplay.musicplatform.catalog.dto.response.SongResponse;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.mapper.SongMapper;
import com.revplay.musicplatform.playback.service.AutoplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/autoplay")
@Tag(name = "Autoplay", description = "Autoplay endpoints")
public class AutoplayController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoplayController.class);

    private final AutoplayService autoplayService;
    private final SongMapper songMapper;

    public AutoplayController(AutoplayService autoplayService, SongMapper songMapper) {
        this.autoplayService = autoplayService;
        this.songMapper = songMapper;
    }

    @GetMapping("/next/{userId}/{songId}")
    @Operation(summary = "Get next autoplay song")
    public ResponseEntity<SongResponse> getNextSong(
            @PathVariable Long userId,
            @PathVariable Long songId
    ) {
        LOGGER.info("Autoplay next request received for userId={}, songId={}", userId, songId);
        Song nextSong = autoplayService.getNextSong(userId, songId);
        return ResponseEntity.ok(songMapper.toResponse(nextSong));
    }
}
