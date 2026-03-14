package com.revplay.musicplatform.catalog.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

@Tag("unit")
class FileStorageServiceTest {

    private static final String SONGS = "songs";
    private static final String PODCASTS = "podcasts";
    private static final String IMAGES = "images";

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("storeSong and loadSong persist and retrieve resource")
    void storeAndLoadSong() throws Exception {
        FileStorageService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "track.mp3", "audio/mpeg", "abc".getBytes());

        String stored = service.storeSong(file);

        assertThat(stored).endsWith(".mp3");
        assertThat(service.loadSong(stored).exists()).isTrue();
    }

    @Test
    @DisplayName("storePodcast accepts supported extension")
    void storePodcast() {
        FileStorageService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "episode.aac", "audio/aac", "abc".getBytes());

        String stored = service.storePodcast(file);

        assertThat(stored).endsWith(".aac");
    }

    @Test
    @DisplayName("storeImage accepts jpg and loadImage works")
    void storeImage() {
        FileStorageService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", "abc".getBytes());

        String stored = service.storeImage(file);

        assertThat(stored).endsWith(".jpg");
        assertThat(service.loadImage(stored).exists()).isTrue();
    }

    @Test
    @DisplayName("storeSong rejects unsupported extension")
    void storeSongUnsupportedExtension() {
        FileStorageService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "track.txt", "text/plain", "abc".getBytes());

        assertThatThrownBy(() -> service.storeSong(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported file type");
    }

    @Test
    @DisplayName("storeImage rejects empty file")
    void storeImageEmptyFile() {
        FileStorageService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.storeImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Image file is required");
    }

    @Test
    @DisplayName("loadSong throws not found for missing file")
    void loadSongMissing() {
        FileStorageService service = service();

        assertThatThrownBy(() -> service.loadSong("missing.mp3"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("File not found");
    }

    @Test
    @DisplayName("deleteSongFile removes existing file")
    void deleteSongFileRemovesFile() throws Exception {
        FileStorageService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "track.mp3", "audio/mpeg", "abc".getBytes());
        String stored = service.storeSong(file);

        service.deleteSongFile(stored);

        Path songPath = tempDir.resolve(SONGS).resolve(stored);
        assertThat(Files.exists(songPath)).isFalse();
    }

    private FileStorageService service() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setBaseDir(tempDir.toString());
        properties.setSongsDir(SONGS);
        properties.setPodcastsDir(PODCASTS);
        properties.setImagesDir(IMAGES);
        return new FileStorageService(properties);
    }
}
