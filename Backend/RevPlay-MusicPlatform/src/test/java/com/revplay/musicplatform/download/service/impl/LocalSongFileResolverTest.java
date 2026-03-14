package com.revplay.musicplatform.download.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

@Tag("unit")
class LocalSongFileResolverTest {

    private static final String SONGS_DIR = "songs";
    private static final String FILE_NAME = "track.mp3";
    private static final String FILE_URL = "/api/v1/files/songs/" + FILE_NAME;
    private static final String NOT_FOUND_MESSAGE = "Song file not found";

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("loadSongResource returns readable resource when file exists")
    void loadSongResourceReturnsResourceWhenFileExists() throws Exception {
        LocalSongFileResolver resolver = resolver();
        Path songsPath = tempDir.resolve(SONGS_DIR);
        Files.createDirectories(songsPath);
        Path file = songsPath.resolve(FILE_NAME);
        Files.writeString(file, "audio-bytes", StandardCharsets.UTF_8);

        Resource resource = resolver.loadSongResource(FILE_URL);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
        assertThat(resource.getFilename()).isEqualTo(FILE_NAME);
    }

    @Test
    @DisplayName("loadSongResource throws when fileUrl is blank")
    void loadSongResourceThrowsWhenFileUrlBlank() {
        LocalSongFileResolver resolver = resolver();

        assertThatThrownBy(() -> resolver.loadSongResource(" "))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(NOT_FOUND_MESSAGE);
    }

    @Test
    @DisplayName("loadSongResource throws when fileUrl has no slash")
    void loadSongResourceThrowsWhenFileUrlHasNoSlash() {
        LocalSongFileResolver resolver = resolver();

        assertThatThrownBy(() -> resolver.loadSongResource(FILE_NAME))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(NOT_FOUND_MESSAGE);
    }

    @Test
    @DisplayName("loadSongResource throws when fileUrl ends with slash")
    void loadSongResourceThrowsWhenFileUrlEndsWithSlash() {
        LocalSongFileResolver resolver = resolver();

        assertThatThrownBy(() -> resolver.loadSongResource("/api/v1/files/songs/"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(NOT_FOUND_MESSAGE);
    }

    @Test
    @DisplayName("loadSongResource throws when resolved file does not exist")
    void loadSongResourceThrowsWhenFileMissing() {
        LocalSongFileResolver resolver = resolver();

        assertThatThrownBy(() -> resolver.loadSongResource(FILE_URL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(NOT_FOUND_MESSAGE);
    }

    private LocalSongFileResolver resolver() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setBaseDir(tempDir.toString());
        properties.setSongsDir(SONGS_DIR);
        return new LocalSongFileResolver(properties);
    }
}
