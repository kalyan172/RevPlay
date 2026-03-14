package com.revplay.musicplatform.catalog.mapper;



import com.revplay.musicplatform.catalog.dto.request.SongCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.SongUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.SongResponse;
import com.revplay.musicplatform.catalog.entity.Song;
import org.springframework.stereotype.Component;

@Component
public class SongMapper {
    public Song toEntity(SongCreateRequest request, Long artistId, String fileUrl) {
        Song song = new Song();
        song.setArtistId(artistId);
        song.setAlbumId(request.getAlbumId());
        song.setTitle(request.getTitle());
        song.setDurationSeconds(request.getDurationSeconds());
        song.setReleaseDate(request.getReleaseDate());
        song.setFileUrl(fileUrl);
        song.setVisibility(request.getVisibility());
        return song;
    }

    public void updateEntity(Song song, SongUpdateRequest request) {
        song.setTitle(request.getTitle());
        song.setDurationSeconds(request.getDurationSeconds());
        song.setAlbumId(request.getAlbumId());
        song.setReleaseDate(request.getReleaseDate());
    }

    public SongResponse toResponse(Song song) {
        SongResponse response = new SongResponse();
        response.setSongId(song.getSongId());
        response.setArtistId(song.getArtistId());
        response.setAlbumId(song.getAlbumId());
        response.setTitle(song.getTitle());
        response.setDurationSeconds(song.getDurationSeconds());
        response.setFileUrl(song.getFileUrl());
        response.setVisibility(song.getVisibility());
        response.setReleaseDate(song.getReleaseDate());
        response.setIsActive(song.getIsActive());
        response.setCreatedAt(song.getCreatedAt());
        return response;
    }
}

