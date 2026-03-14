package com.revplay.musicplatform.catalog.mapper;



import com.revplay.musicplatform.catalog.dto.request.PodcastCategoryCreateRequest;
import com.revplay.musicplatform.catalog.dto.response.PodcastCategoryResponse;
import com.revplay.musicplatform.catalog.entity.PodcastCategory;
import org.springframework.stereotype.Component;

@Component
public class PodcastCategoryMapper {
    public PodcastCategory toEntity(PodcastCategoryCreateRequest request) {
        PodcastCategory category = new PodcastCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return category;
    }

    public PodcastCategoryResponse toResponse(PodcastCategory category) {
        PodcastCategoryResponse response = new PodcastCategoryResponse();
        response.setCategoryId(category.getCategoryId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setCreatedAt(category.getCreatedAt());
        return response;
    }
}

