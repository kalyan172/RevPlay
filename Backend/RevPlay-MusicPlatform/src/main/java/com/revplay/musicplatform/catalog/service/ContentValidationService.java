package com.revplay.musicplatform.catalog.service;

public interface ContentValidationService {

    void validateSongDuration(Integer durationSeconds);

    void validatePodcastEpisodeDuration(Integer durationSeconds);

    void validateAlbumBelongsToArtist(Long albumId, Long artistId);

    void validateUniqueSongTitleWithinAlbum(Long albumId, String title);

    void validateUniqueSongTitleWithinAlbumForUpdate(Long albumId, String title, Long songId);
}


