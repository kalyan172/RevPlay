package com.revplay.musicplatform.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.catalog.dto.request.PodcastCategoryCreateRequest;
import com.revplay.musicplatform.catalog.entity.PodcastCategory;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PodcastCategoryMapperTest {

    private final PodcastCategoryMapper mapper = new PodcastCategoryMapper();

    @Test
    @DisplayName("toEntity maps category create request")
    void toEntityMapsCreateRequest() {
        PodcastCategoryCreateRequest request = new PodcastCategoryCreateRequest();
        request.setName("Tech");
        request.setDescription("Technology");

        PodcastCategory category = mapper.toEntity(request);

        assertThat(category.getName()).isEqualTo("Tech");
        assertThat(category.getDescription()).isEqualTo("Technology");
    }

    @Test
    @DisplayName("toResponse maps category entity")
    void toResponseMapsEntity() {
        PodcastCategory category = new PodcastCategory();
        category.setCategoryId(1L);
        category.setName("Health");
        category.setDescription("Health desc");
        category.setCreatedAt(LocalDateTime.parse("2026-01-01T00:00:00"));

        var response = mapper.toResponse(category);

        assertThat(response.getCategoryId()).isEqualTo(1L);
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-01-01T00:00:00"));
    }
}
