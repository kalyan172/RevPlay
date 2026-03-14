package com.revplay.musicplatform.catalog.controller;



import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;

import com.revplay.musicplatform.common.constants.ApiPaths;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.catalog.dto.request.PodcastCategoryCreateRequest;
import com.revplay.musicplatform.catalog.dto.response.PodcastCategoryResponse;
import com.revplay.musicplatform.catalog.service.PodcastCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PODCAST_CATEGORIES)
@Tag(name = "Podcast Categories", description = "Podcast category management")
public class PodcastCategoryController {
    private final PodcastCategoryService service;

    public PodcastCategoryController(PodcastCategoryService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create podcast category")
    public ResponseEntity<ApiResponse<PodcastCategoryResponse>> create(@Validated @RequestBody PodcastCategoryCreateRequest request) {
        PodcastCategoryResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(success(response, "Podcast category created"));
    }

    @GetMapping
    @Operation(summary = "List podcast categories")
    public ResponseEntity<ApiResponse<List<PodcastCategoryResponse>>> list() {
        List<PodcastCategoryResponse> response = service.list();
        return ResponseEntity.ok(success(response, "Podcast categories fetched"));
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
