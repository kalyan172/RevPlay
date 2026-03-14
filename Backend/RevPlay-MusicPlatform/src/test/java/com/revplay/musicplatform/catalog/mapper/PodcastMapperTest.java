package com.revplay.musicplatform.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.catalog.dto.request.PodcastCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.PodcastUpdateRequest;
import com.revplay.musicplatform.catalog.entity.Podcast;
import com.revplay.musicplatform.catalog.enums.ContentVisibility;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PodcastMapperTest {

    private final PodcastMapper mapper = new PodcastMapper();

    @Test
    @DisplayName("toEntity maps podcast create request")
    void toEntityMapsCreateRequest() {
        PodcastCreateRequest request = new PodcastCreateRequest();
        request.setCategoryId(9L);
        request.setTitle("P");
        request.setDescription("D");
        request.setCoverImageUrl("I");
        request.setVisibility(ContentVisibility.PUBLIC);

        Podcast podcast = mapper.toEntity(request, 5L);

        assertThat(podcast.getArtistId()).isEqualTo(5L);
        assertThat(podcast.getCategoryId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("updateEntity updates podcast fields")
    void updateEntityUpdatesFields() {
        Podcast podcast = new Podcast();
        PodcastUpdateRequest request = new PodcastUpdateRequest();
        request.setCategoryId(2L);
        request.setTitle("Updated");
        request.setDescription("desc");
        request.setCoverImageUrl("new");
        request.setVisibility(ContentVisibility.UNLISTED);

        mapper.updateEntity(podcast, request);

        assertThat(podcast.getTitle()).isEqualTo("Updated");
        assertThat(podcast.getVisibility()).isEqualTo(ContentVisibility.UNLISTED);
    }

    @Test
    @DisplayName("toResponse maps podcast entity")
    void toResponseMapsEntity() {
        Podcast podcast = new Podcast();
        podcast.setPodcastId(1L);
        podcast.setArtistId(2L);
        podcast.setCategoryId(3L);
        podcast.setTitle("T");
        podcast.setDescription("D");
        podcast.setCoverImageUrl("U");
        podcast.setVisibility(ContentVisibility.PUBLIC);
        podcast.setCreatedAt(LocalDateTime.parse("2026-01-01T10:00:00"));

        var response = mapper.toResponse(podcast);

        assertThat(response.getPodcastId()).isEqualTo(1L);
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-01-01T10:00:00"));
    }
}
