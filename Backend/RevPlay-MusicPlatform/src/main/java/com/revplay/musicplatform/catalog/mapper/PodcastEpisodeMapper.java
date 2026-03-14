package com.revplay.musicplatform.catalog.mapper;


import com.revplay.musicplatform.catalog.dto.request.PodcastEpisodeCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.PodcastEpisodeUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.PodcastEpisodeResponse;
import com.revplay.musicplatform.catalog.entity.PodcastEpisode;
import org.springframework.stereotype.Component;

@Component
public class PodcastEpisodeMapper {
    public PodcastEpisode toEntity(PodcastEpisodeCreateRequest request, Long podcastId, String audioUrl) {
        PodcastEpisode episode = new PodcastEpisode();
        episode.setPodcastId(podcastId);
        episode.setTitle(request.getTitle());
        episode.setDurationSeconds(request.getDurationSeconds());
        episode.setReleaseDate(request.getReleaseDate());
        episode.setAudioUrl(audioUrl);
        return episode;
    }

    public void updateEntity(PodcastEpisode episode, PodcastEpisodeUpdateRequest request) {
        episode.setTitle(request.getTitle());
        episode.setDurationSeconds(request.getDurationSeconds());
        episode.setReleaseDate(request.getReleaseDate());
    }

    public PodcastEpisodeResponse toResponse(PodcastEpisode episode) {
        PodcastEpisodeResponse response = new PodcastEpisodeResponse();
        response.setEpisodeId(episode.getEpisodeId());
        response.setPodcastId(episode.getPodcastId());
        response.setTitle(episode.getTitle());
        response.setDurationSeconds(episode.getDurationSeconds());
        response.setReleaseDate(episode.getReleaseDate());
        response.setAudioUrl(episode.getAudioUrl());
        return response;
    }
}
