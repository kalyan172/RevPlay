package com.revplay.musicplatform.catalog.controller;



import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import com.revplay.musicplatform.common.constants.ApiPaths;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.catalog.dto.request.PodcastCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.PodcastUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.PodcastResponse;
import com.revplay.musicplatform.catalog.service.PodcastService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@Tag(name = "Podcasts", description = "Podcast management for artists")
public class PodcastController {
    private final PodcastService service;

    public PodcastController(PodcastService service) {
        this.service = service;
    }

    @PostMapping(ApiPaths.PODCASTS)
    @Operation(summary = "Create podcast")
    public ResponseEntity<ApiResponse<PodcastResponse>> create(@Validated @RequestBody PodcastCreateRequest request) {
        PodcastResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(success(response, "Podcast created"));
    }

    @PutMapping(ApiPaths.PODCASTS + "/{podcastId}")
    @Operation(summary = "Update podcast")
    public ResponseEntity<ApiResponse<PodcastResponse>> update(@PathVariable Long podcastId,
                                                               @Validated @RequestBody PodcastUpdateRequest request) {
        PodcastResponse response = service.update(podcastId, request);
        return ResponseEntity.ok(success(response, "Podcast updated"));
    }

    @GetMapping(ApiPaths.PODCASTS + "/{podcastId}")
    @Operation(summary = "Get podcast")
    public ResponseEntity<ApiResponse<PodcastResponse>> get(@PathVariable Long podcastId) {
        PodcastResponse response = service.get(podcastId);
        return ResponseEntity.ok(success(response, "Podcast fetched"));
    }

    @DeleteMapping(ApiPaths.PODCASTS + "/{podcastId}")
    @Operation(summary = "Delete podcast")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long podcastId) {
        service.delete(podcastId);
        return ResponseEntity.ok(success(null, "Podcast deleted"));
    }

    @GetMapping(ApiPaths.ARTISTS + "/{artistId}/podcasts")
    @Operation(summary = "List podcasts by artist")
    public ResponseEntity<ApiResponse<Page<PodcastResponse>>> listByArtist(@PathVariable Long artistId,
                                                                           Pageable pageable) {
        Page<PodcastResponse> response = service.listByArtist(artistId, pageable);
        return ResponseEntity.ok(success(response, "Podcasts fetched"));
    }

    @GetMapping(ApiPaths.PODCASTS + "/recommended")
    @Operation(summary = "List recommended podcasts")
    public ResponseEntity<ApiResponse<Page<PodcastResponse>>> listRecommended(Pageable pageable) {
        Page<PodcastResponse> response = service.listRecommended(pageable);
        return ResponseEntity.ok(success(response, "Recommended podcasts fetched"));
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

