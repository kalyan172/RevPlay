package com.revplay.musicplatform.playback.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@Tag("unit")
class PlaybackRequestUtilTest {

    @Test
    @DisplayName("resolveClientKey prefers first X-Forwarded-For IP")
    void resolveFromForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.setRemoteAddr("192.168.1.1");

        String key = PlaybackRequestUtil.resolveClientKey(request);

        assertThat(key).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("resolveClientKey uses X-Real-IP when forwarded header absent")
    void resolveFromRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "172.16.0.9");
        request.setRemoteAddr("192.168.1.1");

        String key = PlaybackRequestUtil.resolveClientKey(request);

        assertThat(key).isEqualTo("172.16.0.9");
    }

    @Test
    @DisplayName("resolveClientKey falls back to remote address")
    void resolveFromRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        String key = PlaybackRequestUtil.resolveClientKey(request);

        assertThat(key).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("resolveClientKey returns unknown when no address data exists")
    void resolveUnknownWhenNoData() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(" ");

        String key = PlaybackRequestUtil.resolveClientKey(request);

        assertThat(key).isEqualTo("unknown");
    }
}
