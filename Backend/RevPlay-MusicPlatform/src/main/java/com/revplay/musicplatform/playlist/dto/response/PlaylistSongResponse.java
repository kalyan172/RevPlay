package com.revplay.musicplatform.playlist.dto.response;

import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistSongResponse {

    private Long id;
    private Long playlistId;
    private Long songId;
    private Integer position;
    private LocalDateTime addedAt;
}
