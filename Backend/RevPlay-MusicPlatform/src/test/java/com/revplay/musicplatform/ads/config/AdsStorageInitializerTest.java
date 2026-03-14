package com.revplay.musicplatform.ads.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.config.FileStorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("unit")
class AdsStorageInitializerTest {

    private static final String ADS_DIR = "ads";
    private static final String NESTED_FILE = "existing-file";

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("run creates ads directory when missing")
    void runCreatesAdsDirectoryWhenMissing() throws Exception {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setBaseDir(tempDir.toString());
        properties.setAdsDir(ADS_DIR);
        AdsStorageInitializer initializer = new AdsStorageInitializer(properties);

        initializer.run();

        assertThat(Files.exists(tempDir.resolve(ADS_DIR))).isTrue();
    }

    @Test
    @DisplayName("run throws IOException when target ads path is an existing file")
    void runThrowsWhenAdsPathIsFile() throws Exception {
        Path filePath = tempDir.resolve(NESTED_FILE);
        Files.writeString(filePath, "x");

        FileStorageProperties properties = new FileStorageProperties();
        properties.setBaseDir(filePath.toString());
        properties.setAdsDir(ADS_DIR);
        AdsStorageInitializer initializer = new AdsStorageInitializer(properties);

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IOException.class);
    }
}
