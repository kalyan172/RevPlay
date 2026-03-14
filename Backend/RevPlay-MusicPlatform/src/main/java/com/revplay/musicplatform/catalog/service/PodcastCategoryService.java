package com.revplay.musicplatform.catalog.service;


import java.util.List;

import com.revplay.musicplatform.catalog.dto.request.PodcastCategoryCreateRequest;
import com.revplay.musicplatform.catalog.dto.response.PodcastCategoryResponse;

public interface PodcastCategoryService {

    PodcastCategoryResponse create(PodcastCategoryCreateRequest request);

    List<PodcastCategoryResponse> list();
}

