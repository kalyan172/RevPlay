package com.revplay.musicplatform.catalog.service;

import com.revplay.musicplatform.catalog.dto.request.PodcastEpisodeCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.PodcastEpisodeUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.PodcastEpisodeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface PodcastEpisodeService {

    PodcastEpisodeResponse create(Long podcastId, PodcastEpisodeCreateRequest request, MultipartFile audioFile);

    PodcastEpisodeResponse update(Long podcastId, Long episodeId, PodcastEpisodeUpdateRequest request);

    PodcastEpisodeResponse get(Long podcastId, Long episodeId);

    void delete(Long podcastId, Long episodeId);

    Page<PodcastEpisodeResponse> listByPodcast(Long podcastId, Pageable pageable);

    PodcastEpisodeResponse replaceAudio(Long podcastId, Long episodeId, MultipartFile audioFile);
}
