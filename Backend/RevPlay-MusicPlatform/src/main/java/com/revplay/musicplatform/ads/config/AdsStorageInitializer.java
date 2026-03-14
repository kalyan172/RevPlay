package com.revplay.musicplatform.ads.config;

import com.revplay.musicplatform.config.FileStorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdsStorageInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdsStorageInitializer.class);
    private final FileStorageProperties fileStorageProperties;

    public AdsStorageInitializer(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public void run(String... args) throws Exception {
        Path adsDir = Path.of(fileStorageProperties.getBaseDir(), fileStorageProperties.getAdsDir());
        try {
            Files.createDirectories(adsDir);
            LOGGER.info("Ads storage directory is ready at {}", adsDir.toAbsolutePath());
        } catch (IOException ex) {
            LOGGER.error("Failed to initialize ads storage directory {}", adsDir.toAbsolutePath(), ex);
            throw ex;
        }
    }
}
