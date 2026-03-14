package com.revplay.musicplatform.playlist.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLikeResponse {

    private Long id;
    private Long userId;
    private Long likeableId;
    private String likeableType;
    private LocalDateTime createdAt;
}
