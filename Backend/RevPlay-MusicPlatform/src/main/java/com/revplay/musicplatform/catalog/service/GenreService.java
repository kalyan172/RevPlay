package com.revplay.musicplatform.catalog.service;

import com.revplay.musicplatform.catalog.dto.request.GenreUpsertRequest;
import com.revplay.musicplatform.catalog.dto.response.GenreResponse;
import java.util.List;

public interface GenreService {

    List<GenreResponse> getAll();

    GenreResponse getById(Long genreId);

    GenreResponse create(GenreUpsertRequest request);

    GenreResponse update(Long genreId, GenreUpsertRequest request);

    void delete(Long genreId);
}



