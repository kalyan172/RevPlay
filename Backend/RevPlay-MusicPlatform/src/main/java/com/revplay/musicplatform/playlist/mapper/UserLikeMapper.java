package com.revplay.musicplatform.playlist.mapper;

import com.revplay.musicplatform.playlist.dto.request.LikeRequest;
import com.revplay.musicplatform.playlist.dto.response.UserLikeResponse;
import com.revplay.musicplatform.playlist.entity.UserLike;
import org.springframework.stereotype.Component;


@Component
public class UserLikeMapper {


    public UserLike toEntity(LikeRequest request, Long userId) {
        return UserLike.builder()
                .userId(userId)
                .likeableId(request.getLikeableId())
                .likeableType(request.getLikeableType().toUpperCase())
                .build();
    }


    public UserLikeResponse toResponse(UserLike like) {
        return UserLikeResponse.builder()
                .id(like.getId())
                .userId(like.getUserId())
                .likeableId(like.getLikeableId())
                .likeableType(like.getLikeableType())
                .createdAt(like.getCreatedAt())
                .build();
    }
}
