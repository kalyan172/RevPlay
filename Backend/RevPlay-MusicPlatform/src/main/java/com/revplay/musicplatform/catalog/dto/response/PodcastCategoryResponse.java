package com.revplay.musicplatform.catalog.dto.response;



import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PodcastCategoryResponse {
    private Long categoryId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}

