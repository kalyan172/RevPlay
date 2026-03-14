package com.revplay.musicplatform.playback.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.playback.entity.QueueItem;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class QueueItemMapperTest {

    private final QueueItemMapper mapper = new QueueItemMapper();

    @Test
    @DisplayName("toDto maps queue item entity fields")
    void toDtoMapsFields() {
        QueueItem entity = new QueueItem();
        entity.setQueueId(1L);
        entity.setUserId(2L);
        entity.setSongId(3L);
        entity.setEpisodeId(null);
        entity.setPosition(4);
        entity.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        var dto = mapper.toDto(entity);

        assertThat(dto.queueId()).isEqualTo(1L);
        assertThat(dto.position()).isEqualTo(4);
    }
}
