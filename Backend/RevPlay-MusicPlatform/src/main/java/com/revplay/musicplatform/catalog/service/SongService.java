package com.revplay.musicplatform.catalog.service;

import com.revplay.musicplatform.catalog.dto.request.SongCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.SongUpdateRequest;
import com.revplay.musicplatform.catalog.dto.request.SongVisibilityRequest;
import com.revplay.musicplatform.catalog.dto.response.SongResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface SongService {

    SongResponse create(SongCreateRequest request, MultipartFile audioFile);

    SongResponse update(Long songId, SongUpdateRequest request);

    SongResponse get(Long songId);

    void delete(Long songId);

    Page<SongResponse> listByArtist(Long artistId, Pageable pageable);

    SongResponse updateVisibility(Long songId, SongVisibilityRequest request);

    SongResponse replaceAudio(Long songId, MultipartFile audioFile);
}
