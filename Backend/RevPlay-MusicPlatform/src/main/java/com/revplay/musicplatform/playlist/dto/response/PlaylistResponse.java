package com.revplay.musicplatform.playlist.dto.response;

import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistResponse {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private Boolean isPublic;
    private long songCount;
    private long followerCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
