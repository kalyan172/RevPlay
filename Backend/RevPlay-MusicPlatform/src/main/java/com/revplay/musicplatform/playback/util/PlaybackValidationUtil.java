package com.revplay.musicplatform.playback.util;

import com.revplay.musicplatform.playback.exception.PlaybackValidationException;

public final class PlaybackValidationUtil {

    private PlaybackValidationUtil() {
    }

    public static void requireExactlyOneContentId(Long songId, Long episodeId) {
        boolean hasSong = songId != null;
        boolean hasEpisode = episodeId != null;
        if (hasSong == hasEpisode) {
            throw new PlaybackValidationException("Exactly one of songId or episodeId must be provided");
        }
    }

    public static void requireLimitInRange(int limit, int min, int max) {
        if (limit < min || limit > max) {
            throw new PlaybackValidationException("limit must be between " + min + " and " + max);
        }
    }
}


