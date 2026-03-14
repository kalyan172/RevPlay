package com.revplay.musicplatform.artist.dto.response;

import lombok.Data;

@Data
public class ArtistSummaryResponse {
    private Long artistId;
    private long songCount;
    private long albumCount;
    private long podcastCount;
}

