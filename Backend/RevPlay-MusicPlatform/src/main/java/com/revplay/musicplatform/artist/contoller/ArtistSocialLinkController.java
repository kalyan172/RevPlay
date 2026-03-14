package com.revplay.musicplatform.artist.contoller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;

import com.revplay.musicplatform.common.constants.ApiPaths;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkUpdateRequest;
import com.revplay.musicplatform.artist.dto.response.ArtistSocialLinkResponse;
import com.revplay.musicplatform.artist.service.ArtistSocialLinkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ARTISTS + "/{artistId}/social-links")
@Tag(name = "Artist Social Links", description = "Artist social media links management")
public class ArtistSocialLinkController {
    private final ArtistSocialLinkService service;

    public ArtistSocialLinkController(ArtistSocialLinkService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create artist social link")
    public ResponseEntity<ApiResponse<ArtistSocialLinkResponse>> create(@PathVariable Long artistId,
                                                                        @Validated @RequestBody ArtistSocialLinkCreateRequest request) {
        ArtistSocialLinkResponse response = service.create(artistId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(success(response, "Social link created"));
    }

    @GetMapping
    @Operation(summary = "List artist social links")
    public ResponseEntity<ApiResponse<List<ArtistSocialLinkResponse>>> list(@PathVariable Long artistId) {
        List<ArtistSocialLinkResponse> response = service.list(artistId);
        return ResponseEntity.ok(success(response, "Social links fetched"));
    }

    @PutMapping("/{linkId}")
    @Operation(summary = "Update artist social link")
    public ResponseEntity<ApiResponse<ArtistSocialLinkResponse>> update(@PathVariable Long artistId,
                                                                        @PathVariable Long linkId,
                                                                        @Validated @RequestBody ArtistSocialLinkUpdateRequest request) {
        ArtistSocialLinkResponse response = service.update(artistId, linkId, request);
        return ResponseEntity.ok(success(response, "Social link updated"));
    }

    @DeleteMapping("/{linkId}")
    @Operation(summary = "Delete artist social link")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long artistId, @PathVariable Long linkId) {
        service.delete(artistId, linkId);
        return ResponseEntity.ok(success(null, "Social link deleted"));
    }

    private <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

