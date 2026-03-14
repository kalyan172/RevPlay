package com.revplay.musicplatform.artist.mapper;



import com.revplay.musicplatform.artist.dto.request.ArtistCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistUpdateRequest;
import com.revplay.musicplatform.artist.dto.response.ArtistResponse;
import com.revplay.musicplatform.artist.entity.Artist;
import org.springframework.stereotype.Component;

@Component
public class ArtistMapper {
    public Artist toEntity(ArtistCreateRequest request, Long userId) {
        Artist artist = new Artist();
        artist.setUserId(userId);
        artist.setDisplayName(request.getDisplayName());
        artist.setBio(request.getBio());
        artist.setBannerImageUrl(request.getBannerImageUrl());
        artist.setArtistType(request.getArtistType());
        return artist;
    }

    public void updateEntity(Artist artist, ArtistUpdateRequest request) {
        artist.setDisplayName(request.getDisplayName());
        artist.setBio(request.getBio());
        artist.setBannerImageUrl(request.getBannerImageUrl());
        artist.setArtistType(request.getArtistType());
    }

    public ArtistResponse toResponse(Artist artist) {
        ArtistResponse response = new ArtistResponse();
        response.setArtistId(artist.getArtistId());
        response.setUserId(artist.getUserId());
        response.setDisplayName(artist.getDisplayName());
        response.setBio(artist.getBio());
        response.setBannerImageUrl(artist.getBannerImageUrl());
        response.setArtistType(artist.getArtistType());
        response.setVerified(artist.getVerified());
        response.setCreatedAt(artist.getCreatedAt());
        response.setUpdatedAt(artist.getUpdatedAt());
        return response;
    }
}

