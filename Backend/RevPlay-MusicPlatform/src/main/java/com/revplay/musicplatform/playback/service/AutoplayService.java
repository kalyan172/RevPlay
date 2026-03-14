package com.revplay.musicplatform.playback.service;

import com.revplay.musicplatform.catalog.entity.Song;

public interface AutoplayService {

    Song getNextSong(Long userId, Long currentSongId);
}
