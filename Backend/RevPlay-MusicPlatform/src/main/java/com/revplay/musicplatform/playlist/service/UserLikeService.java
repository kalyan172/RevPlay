package com.revplay.musicplatform.playlist.service;

import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.playlist.dto.request.LikeRequest;
import com.revplay.musicplatform.playlist.dto.response.UserLikeResponse;

public interface UserLikeService {

    UserLikeResponse likeContent(LikeRequest request);

    void unlikeContent(Long likeId);

    PagedResponseDto<UserLikeResponse> getUserLikes(Long userId, String likeableType, int page, int size);
}


