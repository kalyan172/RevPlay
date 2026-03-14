package com.revplay.musicplatform.artist.contoller;



import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.revplay.musicplatform.common.constants.ApiPaths;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.artist.dto.request.ArtistCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistUpdateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistVerifyRequest;
import com.revplay.musicplatform.artist.dto.response.ArtistResponse;
import com.revplay.musicplatform.artist.dto.response.ArtistSummaryResponse;
import com.revplay.musicplatform.artist.service.ArtistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping(ApiPaths.ARTISTS)
@Tag(name = "Artists", description = "Artist profile and summary management")
public class ArtistController {
    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    @PostMapping
    @Operation(summary = "Create artist profile")
    public ResponseEntity<ApiResponse<ArtistResponse>> create(@Validated @RequestBody ArtistCreateRequest request) {
        ArtistResponse response = artistService.createArtist(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(success(response, "Artist created"));
    }

    @PutMapping("/{artistId}")
    @Operation(summary = "Update artist profile")
    public ResponseEntity<ApiResponse<ArtistResponse>> update(@PathVariable Long artistId,
                                                              @Validated @RequestBody ArtistUpdateRequest request) {
        ArtistResponse response = artistService.updateArtist(artistId, request);
        return ResponseEntity.ok(success(response, "Artist updated"));
    }

    @GetMapping("/{artistId}")
    @Operation(summary = "Get artist profile")
    public ResponseEntity<ApiResponse<ArtistResponse>> get(@PathVariable Long artistId) {
        ArtistResponse response = artistService.getArtist(artistId);
        return ResponseEntity.ok(success(response, "Artist fetched"));
    }

    @PatchMapping("/{artistId}/verify")
    @Operation(summary = "Verify or unverify artist (admin)")
    public ResponseEntity<ApiResponse<ArtistResponse>> verify(@PathVariable Long artistId,
                                                              @Validated @RequestBody ArtistVerifyRequest request) {
        ArtistResponse response = artistService.verifyArtist(artistId, request);
        return ResponseEntity.ok(success(response, "Artist verification updated"));
    }

    @GetMapping("/{artistId}/summary")
    @Operation(summary = "Get artist content summary")
    public ResponseEntity<ApiResponse<ArtistSummaryResponse>> summary(@PathVariable Long artistId) {
        ArtistSummaryResponse response = artistService.getSummary(artistId);
        return ResponseEntity.ok(successGeneric(response, "Artist summary fetched"));
    }

    private ApiResponse<ArtistResponse> success(ArtistResponse data, String message) {
        return ApiResponse.<ArtistResponse>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private <T> ApiResponse<T> successGeneric(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

