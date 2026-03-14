package com.revplay.musicplatform.playlist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDetailResponse {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private Boolean isPublic;
    private long songCount;
    private long followerCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PlaylistSongResponse> songs;
}