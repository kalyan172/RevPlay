package com.revplay.musicplatform.playlist.controller;

import com.revplay.musicplatform.common.constants.ApiPaths;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.playlist.dto.request.LikeRequest;
import com.revplay.musicplatform.playlist.dto.response.UserLikeResponse;
import com.revplay.musicplatform.playlist.service.UserLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(ApiPaths.API_V1 + "/likes")
@RequiredArgsConstructor
@Tag(name = "User Likes", description = "Like/unlike songs and podcasts, view user favorites")
public class UserLikeController {

    private final UserLikeService userLikeService;

    @PostMapping
    @Operation(summary = "Like a song or podcast", description = "Uses authenticated user from JWT. likeableType must be SONG or PODCAST. Duplicate likes are rejected.")
    public ResponseEntity<ApiResponse<UserLikeResponse>> likeContent(
            @Valid @RequestBody LikeRequest request) {

        UserLikeResponse response = userLikeService.likeContent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Content liked successfully", response));
    }

    @DeleteMapping("/{likeId}")
    @Operation(summary = "Remove a like (unlike)", description = "Uses authenticated user from JWT. Only owner can remove.")
    public ResponseEntity<ApiResponse<Void>> unlikeContent(
            @PathVariable Long likeId) {

        userLikeService.unlikeContent(likeId);
        return ResponseEntity.ok(ApiResponse.success("Like removed successfully"));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get all likes for a user", description = "Uses authenticated user from JWT. Admin can view any user, others only self.")
    public ResponseEntity<ApiResponse<PagedResponseDto<UserLikeResponse>>> getUserLikes(
            @PathVariable Long userId,
            @RequestParam(required = false) @Parameter(description = "SONG or PODCAST") String likeableType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponseDto<UserLikeResponse> response = userLikeService.getUserLikes(
                userId, likeableType, page, size);
        return ResponseEntity.ok(ApiResponse.success("User likes retrieved", response));
    }
}

