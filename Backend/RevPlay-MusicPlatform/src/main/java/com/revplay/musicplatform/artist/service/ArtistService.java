package com.revplay.musicplatform.artist.service;



import com.revplay.musicplatform.artist.dto.request.ArtistCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistUpdateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistVerifyRequest;
import com.revplay.musicplatform.artist.dto.response.ArtistResponse;
import com.revplay.musicplatform.artist.dto.response.ArtistSummaryResponse;

public interface ArtistService {

    ArtistResponse createArtist(ArtistCreateRequest request);

    ArtistResponse updateArtist(Long artistId, ArtistUpdateRequest request);

    ArtistResponse getArtist(Long artistId);

    ArtistResponse verifyArtist(Long artistId, ArtistVerifyRequest request);

    ArtistSummaryResponse getSummary(Long artistId);
}
