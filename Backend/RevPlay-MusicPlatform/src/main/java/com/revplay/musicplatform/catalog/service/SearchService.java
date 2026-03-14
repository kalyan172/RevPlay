package com.revplay.musicplatform.catalog.service;

import com.revplay.musicplatform.catalog.dto.request.SearchRequest;
import com.revplay.musicplatform.catalog.dto.response.SearchResultItemResponse;
import com.revplay.musicplatform.common.dto.PagedResponseDto;

public interface SearchService {

    PagedResponseDto<SearchResultItemResponse> search(SearchRequest request);
}



