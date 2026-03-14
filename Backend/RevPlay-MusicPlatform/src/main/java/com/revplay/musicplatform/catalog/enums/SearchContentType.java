package com.revplay.musicplatform.catalog.enums;

import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;

public enum SearchContentType {
    SONG,
    ALBUM,
    ARTIST,
    PODCAST,
    PODCAST_EPISODE,
    ALL;

    public static SearchContentType from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        try {
            return SearchContentType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            String normalized = value.trim().toLowerCase();
            if ("episode".equals(normalized) || "podcast-episode".equals(normalized)
                    || "podcastepisode".equals(normalized)) {
                return PODCAST_EPISODE;
            }
            throw new DiscoveryValidationException(
                    "type must be one of: song, album, artist, podcast, podcast_episode, all"
            );
        }
    }
}

