package com.revplay.musicplatform.playlist.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.playlist.dto.request.LikeRequest;
import com.revplay.musicplatform.playlist.entity.UserLike;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class UserLikeMapperTest {

    private final UserLikeMapper mapper = new UserLikeMapper();

    @Test
    @DisplayName("toEntity uppercases likeable type")
    void toEntityUppercasesType() {
        LikeRequest request = new LikeRequest();
        request.setLikeableId(20L);
        request.setLikeableType("song");

        UserLike like = mapper.toEntity(request, 9L);

        assertThat(like.getUserId()).isEqualTo(9L);
        assertThat(like.getLikeableType()).isEqualTo("SONG");
    }

    @Test
    @DisplayName("toResponse maps user like entity")
    void toResponseMapsFields() {
        UserLike like = UserLike.builder()
                .id(1L)
                .userId(2L)
                .likeableId(3L)
                .likeableType("PODCAST")
                .createdAt(LocalDateTime.parse("2026-01-01T00:00:00"))
                .build();

        var response = mapper.toResponse(like);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getLikeableType()).isEqualTo("PODCAST");
    }
}
