package com.revplay.musicplatform.playlist.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ContentReferenceValidationServiceImplTest {

    private static final Long CONTENT_ID = 99L;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ContentReferenceValidationServiceImpl service;

    @Test
    @DisplayName("validation disabled skips DB lookup")
    void validationDisabledSkipsLookup() {
        ReflectionTestUtils.setField(service, "validateContentExistence", false);

        assertThatCode(() -> service.validateLikeTargetExists("SONG", CONTENT_ID)).doesNotThrowAnyException();
        verify(jdbcTemplate, never()).queryForObject(any(String.class), eq(Integer.class), any());
    }

    @Test
    @DisplayName("song target exists passes")
    void songExists() {
        ReflectionTestUtils.setField(service, "validateContentExistence", true);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM songs WHERE song_id = ?", Integer.class, CONTENT_ID)).thenReturn(1);

        assertThatCode(() -> service.validateLikeTargetExists("song", CONTENT_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("podcast target exists passes")
    void podcastExists() {
        ReflectionTestUtils.setField(service, "validateContentExistence", true);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM podcasts WHERE podcast_id = ?", Integer.class, CONTENT_ID)).thenReturn(1);

        assertThatCode(() -> service.validateLikeTargetExists("PODCAST", CONTENT_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("unsupported likeable type throws illegal argument")
    void unsupportedType() {
        ReflectionTestUtils.setField(service, "validateContentExistence", true);

        assertThatThrownBy(() -> service.validateLikeTargetExists("ALBUM", CONTENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported likeableType: ALBUM");
    }

    @Test
    @DisplayName("missing target throws resource not found")
    void missingTarget() {
        ReflectionTestUtils.setField(service, "validateContentExistence", true);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM songs WHERE song_id = ?", Integer.class, CONTENT_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.validateLikeTargetExists("SONG", CONTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("SONG not found with id: " + CONTENT_ID);
    }
}
