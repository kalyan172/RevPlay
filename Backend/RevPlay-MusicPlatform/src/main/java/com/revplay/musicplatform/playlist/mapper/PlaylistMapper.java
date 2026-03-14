package com.revplay.musicplatform.playlist.mapper;

import com.revplay.musicplatform.playlist.dto.request.CreatePlaylistRequest;
import com.revplay.musicplatform.playlist.dto.response.PlaylistDetailResponse;
import com.revplay.musicplatform.playlist.dto.response.PlaylistResponse;
import com.revplay.musicplatform.playlist.entity.Playlist;
import org.springframework.stereotype.Component;

import java.util.Collections;


@Component
public class PlaylistMapper {


    public Playlist toEntity(CreatePlaylistRequest request, Long userId) {
        return Playlist.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .build();
    }


    public PlaylistResponse toResponse(Playlist playlist, long songCount, long followerCount) {
        return PlaylistResponse.builder()
                .id(playlist.getId())
                .userId(playlist.getUserId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .isPublic(playlist.getIsPublic())
                .songCount(songCount)
                .followerCount(followerCount)
                .createdAt(playlist.getCreatedAt())
                .updatedAt(playlist.getUpdatedAt())
                .build();
    }


    public PlaylistDetailResponse toDetailResponse(Playlist playlist, long songCount, long followerCount) {
        return PlaylistDetailResponse.builder()
                .id(playlist.getId())
                .userId(playlist.getUserId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .isPublic(playlist.getIsPublic())
                .songCount(songCount)
                .followerCount(followerCount)
                .createdAt(playlist.getCreatedAt())
                .updatedAt(playlist.getUpdatedAt())
                .songs(Collections.emptyList())
                .build();
    }
}
