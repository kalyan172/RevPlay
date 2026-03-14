package com.revplay.musicplatform.catalog.dto.response;


import java.time.LocalDate;

import lombok.Data;

@Data
public class PodcastEpisodeResponse {
    private Long episodeId;
    private Long podcastId;
    private String title;
    private String audioUrl;
    private Integer durationSeconds;
    private LocalDate releaseDate;
}

