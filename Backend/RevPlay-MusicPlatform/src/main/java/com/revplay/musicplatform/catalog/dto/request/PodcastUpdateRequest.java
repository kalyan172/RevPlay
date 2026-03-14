package com.revplay.musicplatform.catalog.dto.request;



import com.revplay.musicplatform.catalog.enums.ContentVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PodcastUpdateRequest {
    @NotNull
    private Long categoryId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @Size(max = 2048)
    private String coverImageUrl;

    private ContentVisibility visibility;
}

