package com.revplay.musicplatform.catalog.service;



import com.revplay.musicplatform.catalog.dto.request.AlbumCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.AlbumUpdateRequest;
import com.revplay.musicplatform.catalog.dto.response.AlbumResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AlbumService {

    AlbumResponse create(AlbumCreateRequest request);

    AlbumResponse update(Long albumId, AlbumUpdateRequest request);

    AlbumResponse get(Long albumId);

    void delete(Long albumId);

    Page<AlbumResponse> listByArtist(Long artistId, Pageable pageable);

    void addSongToAlbum(Long albumId, Long songId);

    void removeSongFromAlbum(Long albumId, Long songId);
}

