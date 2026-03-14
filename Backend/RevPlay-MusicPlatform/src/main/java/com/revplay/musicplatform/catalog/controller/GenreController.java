package com.revplay.musicplatform.catalog.controller;

import com.revplay.musicplatform.catalog.dto.request.GenreUpsertRequest;
import com.revplay.musicplatform.catalog.dto.response.GenreResponse;
import com.revplay.musicplatform.catalog.service.GenreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/genres")
@Tag(name = "Genres", description = "Genre master data management APIs")
public class GenreController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenreController.class);

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    @Operation(summary = "Get all active genres")
    public ResponseEntity<List<GenreResponse>> getAll() {
        LOGGER.info("Received request to fetch all genres");
        return ResponseEntity.ok(genreService.getAll());
    }

    @GetMapping("/{genreId}")
    @Operation(summary = "Get genre by id")
    public ResponseEntity<GenreResponse> getById(@PathVariable Long genreId) {
        LOGGER.info("Received request to fetch genre by id: {}", genreId);
        return ResponseEntity.ok(genreService.getById(genreId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create genre (Admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<GenreResponse> create(@Valid @RequestBody GenreUpsertRequest request) {
        LOGGER.info("Received request to create genre with name: {}", request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(genreService.create(request));
    }

    @PutMapping("/{genreId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update genre (Admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<GenreResponse> update(
            @PathVariable Long genreId,
            @Valid @RequestBody GenreUpsertRequest request
    ) {
        LOGGER.info("Received request to update genre id: {}", genreId);
        return ResponseEntity.ok(genreService.update(genreId, request));
    }

    @DeleteMapping("/{genreId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete genre (Admin, soft delete)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Void> delete(@PathVariable Long genreId) {
        LOGGER.info("Received request to delete genre id: {}", genreId);
        genreService.delete(genreId);
        return ResponseEntity.noContent().build();
    }
}

