package com.revplay.musicplatform.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.catalog.dto.request.PodcastEpisodeCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.PodcastEpisodeUpdateRequest;
import com.revplay.musicplatform.catalog.entity.PodcastEpisode;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PodcastEpisodeMapperTest {

    private final PodcastEpisodeMapper mapper = new PodcastEpisodeMapper();

    @Test
    @DisplayName("toEntity maps podcast episode create request")
    void toEntityMapsCreateRequest() {
        PodcastEpisodeCreateRequest request = new PodcastEpisodeCreateRequest();
        request.setTitle("Ep");
        request.setDurationSeconds(300);
        request.setReleaseDate(LocalDate.parse("2026-01-01"));

        PodcastEpisode episode = mapper.toEntity(request, 4L, "ep.mp3");

        assertThat(episode.getPodcastId()).isEqualTo(4L);
        assertThat(episode.getAudioUrl()).isEqualTo("ep.mp3");
    }

    @Test
    @DisplayName("updateEntity updates episode fields")
    void updateEntityUpdatesFields() {
        PodcastEpisode episode = new PodcastEpisode();
        PodcastEpisodeUpdateRequest request = new PodcastEpisodeUpdateRequest();
        request.setTitle("Updated");
        request.setDurationSeconds(360);
        request.setReleaseDate(LocalDate.parse("2026-02-02"));

        mapper.updateEntity(episode, request);

        assertThat(episode.getTitle()).isEqualTo("Updated");
        assertThat(episode.getDurationSeconds()).isEqualTo(360);
    }

    @Test
    @DisplayName("toResponse maps episode entity")
    void toResponseMapsEntity() {
        PodcastEpisode episode = new PodcastEpisode();
        episode.setEpisodeId(1L);
        episode.setPodcastId(2L);
        episode.setTitle("T");
        episode.setDurationSeconds(200);
        episode.setReleaseDate(LocalDate.parse("2026-03-03"));
        episode.setAudioUrl("a.mp3");

        var response = mapper.toResponse(episode);

        assertThat(response.getEpisodeId()).isEqualTo(1L);
        assertThat(response.getAudioUrl()).isEqualTo("a.mp3");
    }
}
