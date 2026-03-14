package com.revplay.musicplatform.playlist.mapper;

import com.revplay.musicplatform.playlist.dto.response.PlaylistSongResponse;
import com.revplay.musicplatform.playlist.entity.PlaylistSong;
import org.springframework.stereotype.Component;


@Component
public class PlaylistSongMapper {

    public PlaylistSongResponse toResponse(PlaylistSong song) {
        return PlaylistSongResponse.builder()
                .id(song.getId())
                .playlistId(song.getPlaylistId())
                .songId(song.getSongId())
                .position(song.getPosition())
                .addedAt(song.getAddedAt())
                .build();
    }
}
