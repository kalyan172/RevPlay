package com.revplay.musicplatform.catalog.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@Tag("unit")
class AudioMetadataServiceTest {

    private static final String FILE_PART_NAME = "file";
    private static final String MP3_NAME = "sample.mp3";
    private static final String BINARY_CONTENT_TYPE = "application/octet-stream";
    private static final byte[] INVALID_AUDIO = "not-audio".getBytes();
    private static final int FALLBACK_SECONDS = 180;
    private static final int ZERO_SECONDS = 0;

    private final AudioMetadataService audioMetadataService = new AudioMetadataService();

    @Test
    @DisplayName("resolveDurationSeconds returns fallback when file is null")
    void resolveDurationWithNullFileUsesFallback() {
        Integer duration = audioMetadataService.resolveDurationSeconds(null, FALLBACK_SECONDS);

        assertThat(duration).isEqualTo(FALLBACK_SECONDS);
    }

    @Test
    @DisplayName("resolveDurationSeconds returns fallback when file is empty")
    void resolveDurationWithEmptyFileUsesFallback() {
        MultipartFile emptyFile = new MockMultipartFile(FILE_PART_NAME, MP3_NAME, BINARY_CONTENT_TYPE, new byte[0]);

        Integer duration = audioMetadataService.resolveDurationSeconds(emptyFile, FALLBACK_SECONDS);

        assertThat(duration).isEqualTo(FALLBACK_SECONDS);
    }

    @Test
    @DisplayName("resolveDurationSeconds returns fallback when extraction fails for invalid audio")
    void resolveDurationWithInvalidAudioUsesFallback() {
        MultipartFile invalidAudio = new MockMultipartFile(FILE_PART_NAME, MP3_NAME, BINARY_CONTENT_TYPE, INVALID_AUDIO);

        Integer duration = audioMetadataService.resolveDurationSeconds(invalidAudio, FALLBACK_SECONDS);

        assertThat(duration).isEqualTo(FALLBACK_SECONDS);
    }

    @Test
    @DisplayName("resolveDurationSeconds throws bad request when extraction fails and fallback is missing")
    void resolveDurationWithoutFallbackThrows() {
        MultipartFile invalidAudio = new MockMultipartFile(FILE_PART_NAME, MP3_NAME, BINARY_CONTENT_TYPE, INVALID_AUDIO);

        assertThatThrownBy(() -> audioMetadataService.resolveDurationSeconds(invalidAudio, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unable to determine audio duration; provide durationSeconds in metadata");
    }

    @Test
    @DisplayName("resolveDurationSeconds throws bad request when fallback is non-positive")
    void resolveDurationWithNonPositiveFallbackThrows() {
        assertThatThrownBy(() -> audioMetadataService.resolveDurationSeconds(null, ZERO_SECONDS))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unable to determine audio duration; provide durationSeconds in metadata");
    }
}
