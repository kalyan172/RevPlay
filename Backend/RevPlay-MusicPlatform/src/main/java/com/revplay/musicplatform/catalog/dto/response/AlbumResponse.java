package com.revplay.musicplatform.catalog.dto.response;



import java.time.LocalDate;

import lombok.Data;


@Data
public class AlbumResponse {
    private Long albumId;
    private Long artistId;
    private String title;
    private String description;
    private String coverArtUrl;
    private LocalDate releaseDate;
}

