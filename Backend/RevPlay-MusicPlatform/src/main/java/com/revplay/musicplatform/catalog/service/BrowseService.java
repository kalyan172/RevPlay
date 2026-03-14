package com.revplay.musicplatform.catalog.service;

import com.revplay.musicplatform.catalog.dto.response.NewReleaseItemResponse;
import com.revplay.musicplatform.catalog.dto.response.PopularPodcastItemResponse;
import com.revplay.musicplatform.catalog.dto.response.SearchResultItemResponse;
import com.revplay.musicplatform.catalog.dto.response.TopArtistItemResponse;
import com.revplay.musicplatform.common.dto.PagedResponseDto;

public interface BrowseService {

    PagedResponseDto<NewReleaseItemResponse> newReleases(int page, int size, String sortDir);

    PagedResponseDto<TopArtistItemResponse> topArtists(int page, int size);

    PagedResponseDto<PopularPodcastItemResponse> popularPodcasts(int page, int size);

    PagedResponseDto<SearchResultItemResponse> allSongs(int page, int size, String sortBy, String sortDir);

    PagedResponseDto<SearchResultItemResponse> songsByGenre(Long genreId, int page, int size, String sortBy, String sortDir);
}



