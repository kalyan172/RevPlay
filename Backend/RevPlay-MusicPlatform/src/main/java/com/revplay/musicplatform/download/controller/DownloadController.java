package com.revplay.musicplatform.download.controller;

import com.revplay.musicplatform.download.service.DownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/download")
@RequiredArgsConstructor
@Tag(name = "Song Download", description = "Premium song download APIs")
public class DownloadController {

    private final DownloadService downloadService;

    @GetMapping("/song/{songId}")
    @Operation(summary = "Download song for premium user")
    public ResponseEntity<Resource> downloadSong(
            @PathVariable Long songId,
            @RequestParam Long userId
    ) {
        Resource resource = downloadService.downloadSong(userId, songId);
        String fileName = downloadService.getDownloadFileName(songId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @GetMapping("/status")
    @Operation(summary = "Check if song was already downloaded by user")
    public ResponseEntity<Map<String, Boolean>> downloadStatus(
            @RequestParam Long userId,
            @RequestParam Long songId
    ) {
        return ResponseEntity.ok(Map.of("isDownloaded", downloadService.isDownloaded(userId, songId)));
    }
}

