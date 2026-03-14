package com.revplay.musicplatform.playlist.service;

import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.playlist.dto.response.PlaylistResponse;

public interface PlaylistSearchService {

    PagedResponseDto<PlaylistResponse> searchPublicPlaylists(String keyword, int page, int size);
}

