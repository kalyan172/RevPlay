package com.revplay.musicplatform.catalog.mapper;



import com.revplay.musicplatform.catalog.dto.request.PodcastCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.PodcastUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.PodcastResponse;
import com.revplay.musicplatform.catalog.entity.Podcast;
import org.springframework.stereotype.Component;

@Component
public class PodcastMapper {
    public Podcast toEntity(PodcastCreateRequest request, Long artistId) {
        Podcast podcast = new Podcast();
        podcast.setArtistId(artistId);
        podcast.setCategoryId(request.getCategoryId());
        podcast.setTitle(request.getTitle());
        podcast.setDescription(request.getDescription());
        podcast.setCoverImageUrl(request.getCoverImageUrl());
        podcast.setVisibility(request.getVisibility());
        return podcast;
    }

    public void updateEntity(Podcast podcast, PodcastUpdateRequest request) {
        podcast.setCategoryId(request.getCategoryId());
        podcast.setTitle(request.getTitle());
        podcast.setDescription(request.getDescription());
        podcast.setCoverImageUrl(request.getCoverImageUrl());
        podcast.setVisibility(request.getVisibility());
    }

    public PodcastResponse toResponse(Podcast podcast) {
        PodcastResponse response = new PodcastResponse();
        response.setPodcastId(podcast.getPodcastId());
        response.setArtistId(podcast.getArtistId());
        response.setCategoryId(podcast.getCategoryId());
        response.setTitle(podcast.getTitle());
        response.setDescription(podcast.getDescription());
        response.setCoverImageUrl(podcast.getCoverImageUrl());
        response.setVisibility(podcast.getVisibility());
        response.setCreatedAt(podcast.getCreatedAt());
        return response;
    }
}

