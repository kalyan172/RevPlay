package com.revplay.musicplatform.systemplaylist.service;

import com.revplay.musicplatform.systemplaylist.dto.response.SystemPlaylistResponse;
import java.util.List;

public interface SystemPlaylistService {

    List<SystemPlaylistResponse> getAllActivePlaylists();

    List<Long> getSongIdsBySlug(String slug);

    void addSongsBySlug(String slug, List<Long> songIds);

    void softDeletePlaylist(String slug);
}
