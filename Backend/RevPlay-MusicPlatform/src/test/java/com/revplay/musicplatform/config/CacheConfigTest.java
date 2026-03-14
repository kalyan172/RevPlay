package com.revplay.musicplatform.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@Tag("unit")
class CacheConfigTest {

    private static final List<String> CACHE_NAMES = List.of(
            "genres",
            "browse.newReleases",
            "browse.topArtists",
            "browse.popularPodcasts",
            "browse.allSongs",
            "browse.genreSongs",
            "discover.weekly",
            "discover.feed",
            "analytics.trending",
            "analytics.topArtists",
            "analytics.dashboard",
            "artist.dashboard",
            "artist.popularity"
    );

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    @DisplayName("cache manager contains all expected caches")
    void cacheManagerContainsExpectedCaches() {
        CacheManager cacheManager = cacheConfig.cacheManager();

        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrderElementsOf(CACHE_NAMES);
    }

    @Test
    @DisplayName("cache manager resolves each configured cache")
    void cacheManagerResolvesConfiguredCaches() {
        CacheManager cacheManager = cacheConfig.cacheManager();

        for (String cacheName : CACHE_NAMES) {
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
        }
    }
}
