package com.revplay.musicplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "revplay.upload")
public class FileStorageProperties {
    private String baseDir;
    private String songsDir;
    private String podcastsDir;
    private String imagesDir;
    private String adsDir;
}
