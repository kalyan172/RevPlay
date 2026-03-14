package com.revplay.musicplatform.catalog.dto.response;


import java.time.LocalDate;
import java.time.LocalDateTime;

import com.revplay.musicplatform.catalog.enums.ContentVisibility;

import lombok.Data;

@Data
public class SongResponse {
    private Long songId;
    private Long artistId;
    private Long albumId;
    private String title;
    private Integer durationSeconds;
    private String fileUrl;
    private ContentVisibility visibility;
    private LocalDate releaseDate;
    private Boolean isActive;
    private LocalDateTime createdAt;
}

