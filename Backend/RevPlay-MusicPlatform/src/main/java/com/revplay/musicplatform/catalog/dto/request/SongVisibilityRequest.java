package com.revplay.musicplatform.catalog.dto.request;


import com.revplay.musicplatform.catalog.enums.ContentVisibility;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SongVisibilityRequest {
    private Boolean isActive;

    @NotNull
    private ContentVisibility visibility;
}

