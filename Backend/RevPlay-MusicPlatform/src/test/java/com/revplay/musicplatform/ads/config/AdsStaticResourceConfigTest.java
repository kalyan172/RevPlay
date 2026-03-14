package com.revplay.musicplatform.ads.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.config.FileStorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdsStaticResourceConfigTest {

    private static final String BASE_DIR = "uploads";
    private static final String RESOURCE_PATTERN = "/uploads/**";
    private static final String RESOURCE_LOCATION = "file:uploads/";

    @Mock
    private ResourceHandlerRegistry registry;
    @Mock
    private ResourceHandlerRegistration registration;

    @Test
    @DisplayName("addResourceHandlers registers ads base directory mapping")
    void addResourceHandlersRegistersMapping() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setBaseDir(BASE_DIR);
        AdsStaticResourceConfig config = new AdsStaticResourceConfig(properties);
        when(registry.addResourceHandler(RESOURCE_PATTERN)).thenReturn(registration);

        config.addResourceHandlers(registry);

        verify(registry).addResourceHandler(RESOURCE_PATTERN);
        verify(registration).addResourceLocations(RESOURCE_LOCATION);
    }
}
