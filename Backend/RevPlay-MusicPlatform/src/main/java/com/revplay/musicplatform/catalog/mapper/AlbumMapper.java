package com.revplay.musicplatform.catalog.mapper;



import com.revplay.musicplatform.catalog.dto.request.AlbumCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.AlbumUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.AlbumResponse;
import com.revplay.musicplatform.catalog.entity.Album;
import org.springframework.stereotype.Component;

@Component
public class AlbumMapper {
    public Album toEntity(AlbumCreateRequest request, Long artistId) {
        Album album = new Album();
        album.setArtistId(artistId);
        album.setTitle(request.getTitle());
        album.setDescription(request.getDescription());
        album.setCoverArtUrl(request.getCoverArtUrl());
        album.setReleaseDate(request.getReleaseDate());
        return album;
    }

    public void updateEntity(Album album, AlbumUpdateRequest request) {
        album.setTitle(request.getTitle());
        album.setDescription(request.getDescription());
        album.setCoverArtUrl(request.getCoverArtUrl());
        album.setReleaseDate(request.getReleaseDate());
    }

    public AlbumResponse toResponse(Album album) {
        AlbumResponse response = new AlbumResponse();
        response.setAlbumId(album.getAlbumId());
        response.setArtistId(album.getArtistId());
        response.setTitle(album.getTitle());
        response.setDescription(album.getDescription());
        response.setCoverArtUrl(album.getCoverArtUrl());
        response.setReleaseDate(album.getReleaseDate());
        return response;
    }
}

