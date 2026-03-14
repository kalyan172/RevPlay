package com.revplay.musicplatform.artist.service;



import java.util.List;

import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkUpdateRequest;
import com.revplay.musicplatform.artist.dto.response.ArtistSocialLinkResponse;

public interface ArtistSocialLinkService {

    ArtistSocialLinkResponse create(Long artistId, ArtistSocialLinkCreateRequest request);

    List<ArtistSocialLinkResponse> list(Long artistId);

    ArtistSocialLinkResponse update(Long artistId, Long linkId, ArtistSocialLinkUpdateRequest request);

    void delete(Long artistId, Long linkId);
}

