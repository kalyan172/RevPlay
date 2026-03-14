package com.revplay.musicplatform.catalog.dto.request;


import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SongGenresRequest {
    @NotNull
    @NotEmpty
    private List<Long> genreIds;
}

