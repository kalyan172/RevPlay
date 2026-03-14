package com.revplay.musicplatform.catalog.dto.response;


import java.time.LocalDateTime;

import com.revplay.musicplatform.catalog.enums.ContentVisibility;

import lombok.Data;

@Data
public class PodcastResponse {
    private Long podcastId;
    private Long artistId;
    private Long categoryId;
    private String title;
    private String description;
    private String coverImageUrl;
    private ContentVisibility visibility;
    private LocalDateTime createdAt;
}

