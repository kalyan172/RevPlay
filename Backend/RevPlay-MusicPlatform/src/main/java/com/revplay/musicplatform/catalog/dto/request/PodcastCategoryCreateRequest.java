package com.revplay.musicplatform.catalog.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PodcastCategoryCreateRequest {
    @NotBlank
    private String name;

    private String description;
}

