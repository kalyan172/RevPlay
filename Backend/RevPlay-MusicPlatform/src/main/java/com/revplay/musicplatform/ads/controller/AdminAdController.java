package com.revplay.musicplatform.ads.controller;

import com.revplay.musicplatform.ads.entity.Ad;
import com.revplay.musicplatform.ads.service.AdminAdService;
import com.revplay.musicplatform.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/ads")
@RequiredArgsConstructor
@Tag(name = "Admin Ads", description = "Admin ad upload APIs")
public class AdminAdController {

    private final AdminAdService adminAdService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload audio ad")
    public ResponseEntity<ApiResponse<Ad>> uploadAd(
            @RequestParam String title,
            @RequestParam Integer durationSeconds,
            @RequestParam MultipartFile file
    ) {
        Ad saved = adminAdService.uploadAd(title, file, durationSeconds);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ad uploaded successfully", saved));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate ad")
    public ResponseEntity<ApiResponse<Ad>> deactivateAd(@PathVariable Long id) {
        Ad updated = adminAdService.deactivateAd(id);
        return ResponseEntity.ok(ApiResponse.success("Ad deactivated successfully", updated));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate ad")
    public ResponseEntity<ApiResponse<Ad>> activateAd(@PathVariable Long id) {
        Ad updated = adminAdService.activateAd(id);
        return ResponseEntity.ok(ApiResponse.success("Ad activated successfully", updated));
    }
}
