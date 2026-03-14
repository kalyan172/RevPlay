package com.revplay.musicplatform.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Tag("unit")
class WebConfigTest {

    @Test
    @DisplayName("addResourceHandlers registers api files handler")
    void addResourceHandlersRegistersMapping() {
        WebConfig config = new WebConfig();
        ReflectionTestUtils.setField(config, "uploadDir", "uploads-test");

        StaticWebApplicationContext context = new StaticWebApplicationContext();
        context.setServletContext(new MockServletContext());
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(context, new MockServletContext());

        config.addResourceHandlers(registry);

        assertThat(registry.hasMappingForPattern("/api/v1/files/**")).isTrue();
    }
}
