package com.revplay.musicplatform.ads.config;

import com.revplay.musicplatform.config.FileStorageProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdsStaticResourceConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    public AdsStaticResourceConfig(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseDir = fileStorageProperties.getBaseDir();
        registry.addResourceHandler("/" + baseDir + "/**")
                .addResourceLocations("file:" + baseDir + "/");
    }
}
