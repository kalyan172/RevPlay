package com.revplay.musicplatform.ads.controller;

import com.revplay.musicplatform.ads.entity.Ad;
import com.revplay.musicplatform.ads.service.AdService;
import com.revplay.musicplatform.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
@Tag(name = "Ads", description = "Ad playback APIs")
public class AdsController {

    private final AdService adService;

    @GetMapping("/next")
    @Operation(summary = "Get next ad for playback")
    public ResponseEntity<ApiResponse<Ad>> getNextAd(
            @RequestParam Long userId,
            @RequestParam Long songId
    ) {
        Ad ad = adService.getNextAd(userId, songId);
        return ResponseEntity.ok(ApiResponse.success("Next ad fetched", ad));
    }
}

