package com.revplay.musicplatform.playlist.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistFollowResponse {

    private Long id;
    private Long playlistId;
    private Long followerUserId;
    private LocalDateTime followedAt;
}
